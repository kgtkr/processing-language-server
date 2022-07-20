const path = require("path");
const fs = require("fs/promises");
const { spawn } = require("child_process");
const net = require("net");
const url = require("url");

jest.setTimeout(30000);

class Queue {
  constructor() {
    this.queue = [];
    this.resolves = [];
  }

  push(x) {
    if (this.resolves.length != 0) {
      const resolve = this.resolves.shift();
      resolve(x);
    } else {
      this.queue.push(x);
    }
  }

  async pop() {
    if (this.queue.length === 0) {
      return new Promise((resolve) => {
        this.resolves.push(resolve);
      });
    } else {
      return this.queue.shift();
    }
  }
}

class LSPClient {
  constructor(input, output) {
    this.input = input;
    this.output = output;
    this.queue = new Queue();

    this.output.on("data", (chunk) => {
      const content = chunk.toString();
      for (const line of content.split("\r\n")) {
        if (line.length !== 0 && !line.startsWith("Content-Length: ")) {
          this.queue.push(JSON.parse(line));
        }
      }
    });
  }

  async send(json) {
    const content = JSON.stringify(json);
    await new Promise((resolve) => {
      this.input.write(
        "Content-Length: " + Buffer.from(content).length + "\r\n\r\n" + content,
        resolve
      );
    });
  }

  async receive() {
    return await this.queue.pop();
  }

  destroy() {
    if (!this.input.destroyed) {
      this.input.destroy();
    }
    if (!this.output.destroyed) {
      this.output.destroy();
    }
  }
}

class OutputNormalizer {
  constructor() {
    this.testProjectRoot = path.join(__dirname, "PdeTest");
  }

  static visitJSON(value, f) {
    value = f(value);
    if (typeof value === "object" && value !== null) {
      for (const key of Object.keys(value)) {
        value[key] = this.visitJSON(value[key], f);
      }
    }
    return value;
  }

  static pathToFileURL(path) {
    return url.pathToFileURL(path).href.replace("://", ":");
  }

  convertResult(value) {
    return OutputNormalizer.visitJSON(value, (x) => {
      if (typeof x === "string") {
        return x.replace(
          OutputNormalizer.pathToFileURL(this.testProjectRoot),
          "<testProjectRoot>"
        );
      } else {
        return x;
      }
    });
  }

  getURL(...pathParts) {
    return OutputNormalizer.pathToFileURL(
      path.join(this.testProjectRoot, ...pathParts)
    );
  }
}

describe("e2e", () => {
  let javaPath;
  let javaArgsBase;
  let protocol = process.env.E2E_PROTOCOL;

  beforeAll(async () => {
    const processingPath = path.join(
      __dirname,
      "../cache/processing",
      process.env["TAG"]
    );

    const languageServerPath = path.join(
      __dirname,
      "../target/scala-3.1.0/processing-language-server-assembly-0.1.0-SNAPSHOT.jar"
    );

    let classpath = "";

    const addJars = async (dir) => {
      const classPathSeparator =
        process.env["PROCESSING_OS"] === "windows" ? ";" : ":";
      const absDir = path.join(processingPath, dir);
      const names = await fs.readdir(absDir);
      const jars = names
        .filter((name) => name.endsWith(".jar"))
        .map((name) => path.join(absDir, name));
      for (const jar of jars) {
        classpath += jar + classPathSeparator;
      }
    };

    if (process.env["PROCESSING_OS"] === "macos") {
      await addJars(path.join("Contents", "Java"));
      await addJars(path.join("Contents", "Java", "core", "library"));
      await addJars(path.join("Contents", "Java", "modes", "java", "mode"));
    } else {
      await addJars("lib");
      await addJars(path.join("core", "library"));
      await addJars(path.join("modes", "java", "mode"));
    }

    classpath += languageServerPath;

    javaPath = await (async () => {
      switch (process.env["PROCESSING_OS"]) {
        case "windows":
          return path.join(processingPath, "java", "bin", "java.exe");
        case "linux":
          return path.join(processingPath, "java", "bin", "java");
        case "macos":
          const path1 = path.join(processingPath, "Contents", "PlugIns");
          const path2 = (await fs.readdir(path1))[0];
          const path3 = path.join("Contents", "Home", "bin", "java");
          return path.join(path1, path2, path3);
        default:
          throw new Error("Unsupported platform");
      }
    })();
    javaArgsBase = [
      "-Djna.nosys=true",
      "-Djava.awt.headless=true",
      "-classpath",
      classpath,
      "net.kgtkr.processingLanguageServer.main",
    ];
  });

  const outputNormalizer = new OutputNormalizer();

  let lspClient;
  let languageServerProcess;

  beforeAll(async () => {
    const javaArgs = [...javaArgsBase];
    const port = 32982;
    if (protocol === "tcp") {
      javaArgs.push("tcp", String(port));
    }
    if (protocol === "stdio") {
      javaArgs.push("stdio");
    }
    languageServerProcess = spawn(javaPath, javaArgs);
    languageServerProcess.stderr.on("data", (data) => {
      console.error("[stderr]", data.toString());
    });

    if (protocol === "tcp") {
      await new Promise((resolve) =>
        languageServerProcess.stdout.on("data", async (data) => {
          console.log("[stdout]", data.toString());

          if (data.toString().includes("Ready")) {
            resolve();
          }
        })
      );

      await new Promise((resolve) => {
        const conn = net.connect({ port }, () => {
          resolve();
        });

        conn.on("error", (err) => {
          console.error(err);
        });

        lspClient = new LSPClient(conn, conn);
      });
    }

    if (protocol === "stdio") {
      lspClient = new LSPClient(
        languageServerProcess.stdin,
        languageServerProcess.stdout
      );
    }
  });

  afterAll(() => {
    lspClient.destroy();
    languageServerProcess.kill();
  });

  test("for " + process.env["VERSION"], async () => {
    await lspClient.send({
      jsonrpc: "2.0",
      id: 0,
      method: "initialize",
      params: {
        processId: 27059,
        clientInfo: { name: "Visual Studio Code", version: "1.65.1" },
        locale: "ja",
        rootPath: outputNormalizer.testProjectRoot,
        rootUri: outputNormalizer.getURL(),
        capabilities: {
          workspace: {
            applyEdit: true,
            workspaceEdit: {
              documentChanges: true,
              resourceOperations: ["create", "rename", "delete"],
              failureHandling: "textOnlyTransactional",
              normalizesLineEndings: true,
              changeAnnotationSupport: { groupsOnLabel: true },
            },
            didChangeConfiguration: { dynamicRegistration: true },
            didChangeWatchedFiles: { dynamicRegistration: true },
            symbol: {
              dynamicRegistration: true,
              symbolKind: {
                valueSet: [
                  1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                  19, 20, 21, 22, 23, 24, 25, 26,
                ],
              },
              tagSupport: { valueSet: [1] },
            },
            codeLens: { refreshSupport: true },
            executeCommand: { dynamicRegistration: true },
            configuration: true,
            workspaceFolders: true,
            semanticTokens: { refreshSupport: true },
            fileOperations: {
              dynamicRegistration: true,
              didCreate: true,
              didRename: true,
              didDelete: true,
              willCreate: true,
              willRename: true,
              willDelete: true,
            },
          },
          textDocument: {
            publishDiagnostics: {
              relatedInformation: true,
              versionSupport: false,
              tagSupport: { valueSet: [1, 2] },
              codeDescriptionSupport: true,
              dataSupport: true,
            },
            synchronization: {
              dynamicRegistration: true,
              willSave: true,
              willSaveWaitUntil: true,
              didSave: true,
            },
            completion: {
              dynamicRegistration: true,
              contextSupport: true,
              completionItem: {
                snippetSupport: true,
                commitCharactersSupport: true,
                documentationFormat: ["markdown", "plaintext"],
                deprecatedSupport: true,
                preselectSupport: true,
                tagSupport: { valueSet: [1] },
                insertReplaceSupport: true,
                resolveSupport: {
                  properties: [
                    "documentation",
                    "detail",
                    "additionalTextEdits",
                  ],
                },
                insertTextModeSupport: { valueSet: [1, 2] },
              },
              completionItemKind: {
                valueSet: [
                  1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                  19, 20, 21, 22, 23, 24, 25,
                ],
              },
            },
            hover: {
              dynamicRegistration: true,
              contentFormat: ["markdown", "plaintext"],
            },
            signatureHelp: {
              dynamicRegistration: true,
              signatureInformation: {
                documentationFormat: ["markdown", "plaintext"],
                parameterInformation: { labelOffsetSupport: true },
                activeParameterSupport: true,
              },
              contextSupport: true,
            },
            definition: { dynamicRegistration: true, linkSupport: true },
            references: { dynamicRegistration: true },
            documentHighlight: { dynamicRegistration: true },
            documentSymbol: {
              dynamicRegistration: true,
              symbolKind: {
                valueSet: [
                  1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                  19, 20, 21, 22, 23, 24, 25, 26,
                ],
              },
              hierarchicalDocumentSymbolSupport: true,
              tagSupport: { valueSet: [1] },
              labelSupport: true,
            },
            codeAction: {
              dynamicRegistration: true,
              isPreferredSupport: true,
              disabledSupport: true,
              dataSupport: true,
              resolveSupport: { properties: ["edit"] },
              codeActionLiteralSupport: {
                codeActionKind: {
                  valueSet: [
                    "",
                    "quickfix",
                    "refactor",
                    "refactor.extract",
                    "refactor.inline",
                    "refactor.rewrite",
                    "source",
                    "source.organizeImports",
                  ],
                },
              },
              honorsChangeAnnotations: false,
            },
            codeLens: { dynamicRegistration: true },
            formatting: { dynamicRegistration: true },
            rangeFormatting: { dynamicRegistration: true },
            onTypeFormatting: { dynamicRegistration: true },
            rename: {
              dynamicRegistration: true,
              prepareSupport: true,
              prepareSupportDefaultBehavior: 1,
              honorsChangeAnnotations: true,
            },
            documentLink: {
              dynamicRegistration: true,
              tooltipSupport: true,
            },
            typeDefinition: {
              dynamicRegistration: true,
              linkSupport: true,
            },
            implementation: {
              dynamicRegistration: true,
              linkSupport: true,
            },
            colorProvider: { dynamicRegistration: true },
            foldingRange: {
              dynamicRegistration: true,
              rangeLimit: 5000,
              lineFoldingOnly: true,
            },
            declaration: { dynamicRegistration: true, linkSupport: true },
            selectionRange: { dynamicRegistration: true },
            callHierarchy: { dynamicRegistration: true },
            semanticTokens: {
              dynamicRegistration: true,
              tokenTypes: [
                "namespace",
                "type",
                "class",
                "enum",
                "interface",
                "struct",
                "typeParameter",
                "parameter",
                "variable",
                "property",
                "enumMember",
                "event",
                "function",
                "method",
                "macro",
                "keyword",
                "modifier",
                "comment",
                "string",
                "number",
                "regexp",
                "operator",
              ],
              tokenModifiers: [
                "declaration",
                "definition",
                "readonly",
                "static",
                "deprecated",
                "abstract",
                "async",
                "modification",
                "documentation",
                "defaultLibrary",
              ],
              formats: ["relative"],
              requests: { range: true, full: { delta: true } },
              multilineTokenSupport: false,
              overlappingTokenSupport: false,
            },
            linkedEditingRange: { dynamicRegistration: true },
          },
          window: {
            showMessage: {
              messageActionItem: { additionalPropertiesSupport: true },
            },
            showDocument: { support: true },
            workDoneProgress: true,
          },
          general: {
            regularExpressions: { engine: "ECMAScript", version: "ES2020" },
            markdown: { parser: "marked", version: "1.1.0" },
          },
        },
        trace: "off",
        workspaceFolders: [
          {
            uri: outputNormalizer.getURL(),
            name: "PdeTest",
          },
        ],
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();

    await lspClient.send({
      jsonrpc: "2.0",
      method: "initialized",
      params: {},
    });
    await lspClient.send({
      jsonrpc: "2.0",
      method: "textDocument/didOpen",
      params: {
        textDocument: {
          uri: outputNormalizer.getURL("PdeTest.pde"),
          languageId: "processing",
          version: 1,
          text: "class Hoge {\n  void f(int x) {}\n}\n\nvoid main() {\nHoge hoge = new Hoge();\nhoge\n}\n",
        },
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();

    await lspClient.send({
      jsonrpc: "2.0",
      method: "textDocument/didChange",
      params: {
        textDocument: {
          uri: outputNormalizer.getURL("PdeTest.pde"),
          version: 2,
        },
        contentChanges: [
          {
            text: "class Hoge {\n  void f(int x) {}\n}\n\nvoid main() {\nHoge hoge = new Hoge();\nhoge.\n}\n",
          },
        ],
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();

    await lspClient.send({
      jsonrpc: "2.0",
      id: 1,
      method: "textDocument/completion",
      params: {
        textDocument: {
          uri: outputNormalizer.getURL("PdeTest.pde"),
        },
        position: { line: 6, character: 5 },
        context: { triggerKind: 2, triggerCharacter: "." },
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();

    await lspClient.send({
      jsonrpc: "2.0",
      id: 2,
      method: "completionItem/resolve",
      params: {
        label: "f",
        detail: "f(int x) : void",
        insertTextFormat: 2,
        insertText: "f($1)",
        kind: 2,
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();

    await lspClient.send({
      jsonrpc: "2.0",
      method: "textDocument/didChange",
      params: {
        textDocument: {
          uri: outputNormalizer.getURL("PdeTest.pde"),
          version: 3,
        },
        contentChanges: [
          {
            text: "class Hoge {\n  void f(int x) {}\n}\n\nvoid main() {\nHoge hoge = new Hoge();\nhoge.f()\n}\n",
          },
        ],
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();

    await lspClient.send({
      jsonrpc: "2.0",
      method: "textDocument/didChange",
      params: {
        textDocument: {
          uri: outputNormalizer.getURL("PdeTest.pde"),
          version: 4,
        },
        contentChanges: [
          {
            text: "class Hoge {\n  void f(int x) {}\n}\n\nvoid main() {\nHoge hoge = new Hoge();\nhoge.f();\n}\n",
          },
        ],
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();

    await lspClient.send({
      jsonrpc: "2.0",
      id: 3,
      method: "textDocument/formatting",
      params: {
        textDocument: {
          uri: outputNormalizer.getURL("PdeTest.pde"),
        },
        options: {
          tabSize: 2,
          insertSpaces: true,
          insertFinalNewline: true,
        },
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();

    await lspClient.send({
      jsonrpc: "2.0",
      method: "textDocument/didChange",
      params: {
        textDocument: {
          uri: outputNormalizer.getURL("PdeTest.pde"),
          version: 5,
        },
        contentChanges: [
          {
            text: "class Hoge {\n  void f(int x) {\n  }\n}\n\nvoid main() {\n  Hoge hoge = new Hoge();\n  hoge.f();\n}\n",
          },
        ],
      },
    });

    expect(
      outputNormalizer.convertResult(await lspClient.receive())
    ).toMatchSnapshot();
  });
});

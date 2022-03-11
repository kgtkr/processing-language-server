const path = require("path");
const fs = require("fs");
const { spawn } = require("child_process");
const net = require("net");

const testProjectRoot = path.join(__dirname, "PdeTest");

const processingRoot = path.join(
  __dirname,
  "../cache/processing",
  process.env["TAG"]
);

const languageServerJar = path.join(
  __dirname,
  "../target/scala-3.1.0/processing-language-server-assembly-0.1.0-SNAPSHOT.jar"
);

// macでもローカルテストできるように
const javaPath =
  process.platform === "darwin"
    ? "java"
    : path.join(processingRoot, "java/bin/java");

let classpath = "";

for (const dir of ["lib", "core/library", "modes/java/mode"]) {
  const absDir = path.join(processingRoot, dir);
  const names = fs.readdirSync(absDir);
  const jars = names
    .filter((name) => name.endsWith(".jar"))
    .map((name) => path.join(absDir, name));
  for (const jar of jars) {
    classpath += jar + ":";
  }
}

classpath += languageServerJar;

const port = 32982;
this.languageServerProcess = spawn(javaPath, [
  "-Djna.nosys=true",
  "-classpath",
  classpath,
  "net.kgtkr.processingLanguageServer.main",
  String(port),
]);

let ready = false;

this.languageServerProcess.stderr.on("data", (data) => {
  console.error("[stderr]", data.toString());
});

this.languageServerProcess.stdout.on("data", async (data) => {
  console.log("[stdout]", data.toString());
  if (!ready && data.toString().includes("Ready")) {
    ready = true;

    const conn = net.connect({ port });

    const inputs = [
      {
        jsonrpc: "2.0",
        id: 0,
        method: "initialize",
        params: {
          processId: 27059,
          clientInfo: { name: "Visual Studio Code", version: "1.65.1" },
          locale: "ja",
          rootPath: testProjectRoot,
          rootUri: "file:" + testProjectRoot,
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
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
                    18, 19, 20, 21, 22, 23, 24, 25, 26,
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
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
                    18, 19, 20, 21, 22, 23, 24, 25,
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
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
                    18, 19, 20, 21, 22, 23, 24, 25, 26,
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
              documentLink: { dynamicRegistration: true, tooltipSupport: true },
              typeDefinition: { dynamicRegistration: true, linkSupport: true },
              implementation: { dynamicRegistration: true, linkSupport: true },
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
              uri: "file:" + testProjectRoot,
              name: "PdeTest",
            },
          ],
        },
      },
      { jsonrpc: "2.0", method: "initialized", params: {} },
      {
        jsonrpc: "2.0",
        method: "textDocument/didOpen",
        params: {
          textDocument: {
            uri: "file:" + testProjectRoot + "/PdeTest.pde",
            languageId: "processing",
            version: 1,
            text: "class Hoge {\n  void f(int x) {}\n}\n\nvoid main() {\nHoge hoge = new Hoge();\nhoge\n}\n",
          },
        },
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/didChange",
        params: {
          textDocument: {
            uri: "file:" + testProjectRoot + "/PdeTest.pde",
            version: 2,
          },
          contentChanges: [
            {
              text: "class Hoge {\n  void f(int x) {}\n}\n\nvoid main() {\nHoge hoge = new Hoge();\nhoge.\n}\n",
            },
          ],
        },
      },
      {
        jsonrpc: "2.0",
        id: 1,
        method: "textDocument/completion",
        params: {
          textDocument: {
            uri: "file:" + testProjectRoot + "/PdeTest.pde",
          },
          position: { line: 6, character: 5 },
          context: { triggerKind: 2, triggerCharacter: "." },
        },
      },
      {
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
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/didChange",
        params: {
          textDocument: {
            uri: "file:" + testProjectRoot + "/PdeTest.pde",
            version: 3,
          },
          contentChanges: [
            {
              text: "class Hoge {\n  void f(int x) {}\n}\n\nvoid main() {\nHoge hoge = new Hoge();\nhoge.f()\n}\n",
            },
          ],
        },
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/didChange",
        params: {
          textDocument: {
            uri: "file:" + testProjectRoot + "/PdeTest.pde",
            version: 4,
          },
          contentChanges: [
            {
              text: "class Hoge {\n  void f(int x) {}\n}\n\nvoid main() {\nHoge hoge = new Hoge();\nhoge.f();\n}\n",
            },
          ],
        },
      },
      {
        jsonrpc: "2.0",
        id: 3,
        method: "textDocument/formatting",
        params: {
          textDocument: {
            uri: "file:" + testProjectRoot + "/PdeTest.pde",
          },
          options: { tabSize: 2, insertSpaces: true, insertFinalNewline: true },
        },
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/didChange",
        params: {
          textDocument: {
            uri: "file:" + testProjectRoot + "/PdeTest.pde",
            version: 5,
          },
          contentChanges: [
            {
              text: "class Hoge {\n  void f(int x) {\n  }\n}\n\nvoid main() {\n  Hoge hoge = new Hoge();\n  hoge.f();\n}\n",
            },
          ],
        },
      },
    ];
    const outputs = [
      {
        jsonrpc: "2.0",
        id: 0,
        result: {
          capabilities: {
            textDocumentSync: 1,
            completionProvider: {
              resolveProvider: true,
              triggerCharacters: ["."],
            },
            documentFormattingProvider: true,
          },
        },
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/publishDiagnostics",
        params: {
          uri: "file:" + testProjectRoot + "/PdeTest.pde",
          diagnostics: [
            {
              range: {
                start: { line: 7, character: 7 },
                end: { line: 7, character: 8 },
              },
              severity: 1,
              message: "Syntax Error - Missing name or ; near ‘}’?",
            },
          ],
        },
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/publishDiagnostics",
        params: {
          uri: "file:" + testProjectRoot + "/PdeTest.pde",
          diagnostics: [
            {
              range: {
                start: { line: 7, character: 7 },
                end: { line: 7, character: 8 },
              },
              severity: 1,
              message: "Syntax Error - Missing name or ; near ‘}’?",
            },
          ],
        },
      },
      {
        jsonrpc: "2.0",
        id: 1,
        result: [
          {
            label: "f",
            kind: 2,
            detail: "f(int x) : void",
            insertText: "f($1)",
            insertTextFormat: 2,
          },
          {
            label: "main",
            kind: 2,
            detail: "main() : void",
            insertText: "main()",
            insertTextFormat: 2,
          },
          {
            label: "equals",
            kind: 3,
            detail: "equals(Object) : boolean - Object",
            insertText: "equals($1)",
            insertTextFormat: 2,
          },
          {
            label: "getClass",
            kind: 3,
            detail: "getClass() : Class - Object",
            insertText: "getClass()",
            insertTextFormat: 2,
          },
          {
            label: "hashCode",
            kind: 3,
            detail: "hashCode() : int - Object",
            insertText: "hashCode()",
            insertTextFormat: 2,
          },
          {
            label: "notify",
            kind: 3,
            detail: "notify() : void - Object",
            insertText: "notify()",
            insertTextFormat: 2,
          },
          {
            label: "notifyAll",
            kind: 3,
            detail: "notifyAll() : void - Object",
            insertText: "notifyAll()",
            insertTextFormat: 2,
          },
          {
            label: "toString",
            kind: 3,
            detail: "toString() : String - Object",
            insertText: "toString()",
            insertTextFormat: 2,
          },
          {
            label: "wait",
            kind: 3,
            detail: "wait(long,int) : void - Object",
            insertText: "wait($1,$2)",
            insertTextFormat: 2,
          },
        ],
      },
      {
        jsonrpc: "2.0",
        id: 2,
        result: {
          label: "f",
          kind: 2,
          detail: "f(int x) : void",
          insertText: "f($1)",
          insertTextFormat: 2,
        },
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/publishDiagnostics",
        params: {
          uri: "file:" + testProjectRoot + "/PdeTest.pde",
          diagnostics: [
            {
              range: {
                start: { line: 7, character: 7 },
                end: { line: 7, character: 8 },
              },
              severity: 1,
              message: "Syntax Error - Missing name or ; near ‘}’?",
            },
          ],
        },
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/publishDiagnostics",
        params: {
          uri: "file:" + testProjectRoot + "/PdeTest.pde",
          diagnostics: [
            {
              range: {
                start: { line: 1, character: 13 },
                end: { line: 1, character: 14 },
              },
              severity: 2,
              message: "The value of the parameter x is not used",
            },
            {
              range: {
                start: { line: 6, character: 5 },
                end: { line: 6, character: 6 },
              },
              severity: 1,
              message: "The function “f()” expects parameters like: “f(int)”",
            },
          ],
        },
      },
      {
        jsonrpc: "2.0",
        id: 3,
        result: [
          {
            range: {
              start: { line: 0, character: 0 },
              end: { line: 8, character: 1 },
            },
            newText:
              "class Hoge {\n  void f(int x) {\n  }\n}\n\nvoid main() {\n  Hoge hoge \u003d new Hoge();\n  hoge.f();\n}\n",
          },
        ],
      },
      {
        jsonrpc: "2.0",
        method: "textDocument/publishDiagnostics",
        params: {
          uri: "file:" + testProjectRoot + "/PdeTest.pde",
          diagnostics: [
            {
              range: {
                start: { line: 1, character: 13 },
                end: { line: 1, character: 14 },
              },
              severity: 2,
              message: "The value of the parameter x is not used",
            },
            {
              range: {
                start: { line: 7, character: 7 },
                end: { line: 7, character: 8 },
              },
              severity: 1,
              message: "The function “f()” expects parameters like: “f(int)”",
            },
          ],
        },
      },
    ];

    let actualOutput = "";

    conn.on("data", (chunk) => {
      actualOutput += chunk + "\n";
    });

    for (const input of inputs) {
      const content = JSON.stringify(input);
      conn.write(
        "Content-Length: " + Buffer.from(content).length + "\r\n\r\n" + content
      );
      await new Promise((resolve) => setTimeout(resolve, 1000));
    }

    let expectedOutput = "";
    for (const output of outputs) {
      const content = JSON.stringify(output).replaceAll("=", "\\u003d");
      expectedOutput +=
        "Content-Length: " +
        Buffer.from(content).length +
        "\r\n\r\n" +
        content +
        "\n";
    }

    if (expectedOutput !== actualOutput) {
      console.log("Error");
      console.log("--- Expected Output ---");
      console.log(expectedOutput);
      console.log("--- Actual Output ---");
      console.log(actualOutput);
      process.exit(1);
    } else {
      console.log("OK");
      process.exit(0);
    }
  }
});

setTimeout(() => {
  if (!ready) {
    console.error("language server not ready");
    process.exit(1);
  }
}, 5000);

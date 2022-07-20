const path = require("path");

module.exports = {
  resolveSnapshotPath: (testPath, snapshotExtension) => {
    console.log(testPath, snapshotExtension);
    return path.join(
      path.dirname(testPath),
      "__snapshots__",
      process.env["VERSION"],
      path.basename(testPath) + snapshotExtension
    );
  },
  resolveTestPath: (snapshotFilePath, snapshotExtension) => {
    console.log(snapshotFilePath, snapshotExtension);
    return path.join(
      path.dirname(snapshotFilePath),
      "..",
      "..",
      path.basename(snapshotFilePath, snapshotExtension)
    );
  },
  testPathForConsistencyCheck: "e2e.test.js",
};

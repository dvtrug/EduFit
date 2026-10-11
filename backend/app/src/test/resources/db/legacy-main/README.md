# Historical main migration fixture

Immutable SQL snapshots used only by FlywayMigrationIT, outside the production db/migration location.

V1.04/V1.08 are the original schema/audit scripts; V1.09 is the main seed from commit 6755121122c79dc9705282225d14ffb76e17c29a (checksum -1594466614).
Other unchanged V1.01–V1.07 scripts are shared with production locations explicitly selected in the test.
The fixture recreates the database history that failed with Connection V1.09, then verifies upgrading through V1.10/V1.11 without repair, data loss or seed overwrites.

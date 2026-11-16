-- SMS Read: SQL Server 2022 initial schema.
-- Execute only in a dedicated DEVELOPMENT database after review.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF SCHEMA_ID(N'finance') IS NULL EXEC(N'CREATE SCHEMA finance');

CREATE TABLE finance.AppUser (
    UserId UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_AppUser PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_AppUser_Created DEFAULT SYSUTCDATETIME()
);

CREATE TABLE finance.Device (
    DeviceId UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_Device PRIMARY KEY,
    UserId UNIQUEIDENTIFIER NOT NULL,
    RegisteredAtUtc DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    RevokedAtUtc DATETIME2(3) NULL,
    CONSTRAINT FK_Device_User FOREIGN KEY (UserId) REFERENCES finance.AppUser(UserId),
    CONSTRAINT UQ_Device_User UNIQUE (DeviceId, UserId)
);

CREATE TABLE finance.Bank (
    BankCode VARCHAR(32) NOT NULL CONSTRAINT PK_Bank PRIMARY KEY,
    DisplayNameFa NVARCHAR(100) NOT NULL
);
INSERT INTO finance.Bank (BankCode, DisplayNameFa) VALUES
('MELLI',N'ملی'),('MELLAT',N'ملت'),('SADERAT',N'صادرات'),
('PARSIAN',N'پارسیان'),('SAMAN',N'سامان'),('PASARGAD',N'پاسارگاد'),
('TEJARAT',N'تجارت'),('KESHAVARZI',N'کشاورزی');

CREATE TABLE finance.Account (
    AccountId UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_Account PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    UserId UNIQUEIDENTIFIER NOT NULL,
    BankCode VARCHAR(32) NOT NULL,
    AccountIdentifierCipher VARBINARY(MAX) NULL,
    IdentifierLookupHash BINARY(32) NULL,
    DisplayHint NVARCHAR(32) NULL,
    CreatedAtUtc DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Account_User FOREIGN KEY (UserId) REFERENCES finance.AppUser(UserId),
    CONSTRAINT FK_Account_Bank FOREIGN KEY (BankCode) REFERENCES finance.Bank(BankCode),
    CONSTRAINT UQ_Account_User UNIQUE (AccountId, UserId)
);
CREATE UNIQUE INDEX UX_Account_Lookup ON finance.Account(UserId,BankCode,IdentifierLookupHash)
WHERE IdentifierLookupHash IS NOT NULL;

CREATE TABLE finance.SmsTransaction (
    TransactionId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SmsTransaction PRIMARY KEY,
    UserId UNIQUEIDENTIFIER NOT NULL,
    DeviceId UNIQUEIDENTIFIER NOT NULL,
    EventId UNIQUEIDENTIFIER NOT NULL,
    AccountId UNIQUEIDENTIFIER NULL,
    BankCode VARCHAR(32) NULL,
    Direction VARCHAR(12) NULL,
    AmountIrr BIGINT NULL,
    BalanceIrr BIGINT NULL,
    OccurredAtUtc DATETIME2(3) NULL,
    OccurredAtJalali NVARCHAR(32) NULL,
    TimestampSource VARCHAR(12) NOT NULL,
    ReceivedAtUtc DATETIME2(3) NOT NULL,
    SenderCipher VARBINARY(MAX) NULL,
    RawSmsCipher VARBINARY(MAX) NOT NULL,
    PayloadHash BINARY(32) NOT NULL,
    ParserVersion VARCHAR(50) NULL,
    ParseStatus VARCHAR(12) NOT NULL,
    Confidence DECIMAL(4,3) NULL,
    InsertedAtUtc DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_Transaction_Event UNIQUE(UserId,EventId),
    CONSTRAINT FK_Transaction_Device FOREIGN KEY(DeviceId,UserId) REFERENCES finance.Device(DeviceId,UserId),
    CONSTRAINT FK_Transaction_Account FOREIGN KEY(AccountId,UserId) REFERENCES finance.Account(AccountId,UserId),
    CONSTRAINT FK_Transaction_Bank FOREIGN KEY(BankCode) REFERENCES finance.Bank(BankCode),
    CONSTRAINT CK_Transaction_Direction CHECK(Direction IN ('DEPOSIT','WITHDRAWAL') OR Direction IS NULL),
    CONSTRAINT CK_Transaction_Amount CHECK(AmountIrr >= 0 OR AmountIrr IS NULL),
    CONSTRAINT CK_Transaction_Balance CHECK(BalanceIrr >= 0 OR BalanceIrr IS NULL),
    CONSTRAINT CK_Transaction_TimeSource CHECK(TimestampSource IN ('BANK','RECEIVED','UNKNOWN')),
    CONSTRAINT CK_Transaction_Status CHECK(ParseStatus IN ('PARSED','REVIEW','REJECTED')),
    CONSTRAINT CK_Transaction_Confidence CHECK(Confidence BETWEEN 0 AND 1 OR Confidence IS NULL)
);
CREATE INDEX IX_Transaction_UserTime ON finance.SmsTransaction(UserId,OccurredAtUtc DESC,TransactionId DESC);
CREATE INDEX IX_Transaction_AccountTime ON finance.SmsTransaction(AccountId,OccurredAtUtc DESC,TransactionId DESC) WHERE AccountId IS NOT NULL;

CREATE TABLE finance.AccountBalance (
    AccountId UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_AccountBalance PRIMARY KEY,
    SourceTransactionId BIGINT NOT NULL,
    BalanceIrr BIGINT NOT NULL,
    AsOfUtc DATETIME2(3) NOT NULL,
    UpdatedAtUtc DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Balance_Account FOREIGN KEY(AccountId) REFERENCES finance.Account(AccountId),
    CONSTRAINT FK_Balance_Source FOREIGN KEY(SourceTransactionId) REFERENCES finance.SmsTransaction(TransactionId),
    CONSTRAINT CK_Balance_Nonnegative CHECK(BalanceIrr >= 0)
);

COMMIT TRANSACTION;
-- API must enforce ownership, idempotency and monotonic trusted balance updates
-- inside a serializable transaction or using per-account locking.

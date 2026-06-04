# Ke hoach trien khai thong ke phi gas theo role

## 1. Muc tieu

Xay dung chuc nang ghi nhan va thong ke chi phi gas blockchain cho hai role:

- `SUPPLIER`
- `MANUFACTURER`

He thong phai thong ke duoc ca giao dich thanh cong va giao dich that bai.

Quy tac phan bo chi phi:

- Ghi lo nguyen lieu: `SUPPLIER` tao lo chiu chi phi.
- Ghi lo san xuat/pallet: `MANUFACTURER` tao lo chiu chi phi.
- `changeOwner`: nguoi ban chiu chi phi, khong phu thuoc ai bam xac nhan giao hang.
- `TRANSPORTER` chi cung cap dich vu van chuyen, khong bi tinh phi gas.
- Cac thao tac chi doc/xac minh blockchain khong ton gas va khong ghi vao thong ke.
- Chi phi deploy contract la chi phi he thong, khong phan bo cho supplier/manufacturer.

> Luu y: hien tai moi giao dich blockchain deu duoc ky boi mot system wallet. Vi vay, chuc nang nay la thong ke va phan bo chi phi noi bo, khong phai tru tien truc tiep tu vi rieng cua supplier/manufacturer.

## 2. Nguyen tac tinh phi

### 2.1 Cong thuc

Chi phi thuc te cua mot giao dich:

```text
feeWei = gasUsed * effectiveGasPriceWei
```

Trong do:

- `gasUsed`: luong gas giao dich thuc su da dung, lay tu transaction receipt.
- `effectiveGasPriceWei`: gia gas thuc su cua giao dich.
- `gasLimit`: chi la gioi han toi da, khong duoc dung de tinh chi phi thuc te.

Du lieu tien te phai luu bang `NUMERIC`/`BigInteger`, khong dung `float` hoac `double`.

### 2.2 Phan loai ket qua giao dich

| Trang thai | Mo ta | Co tinh vao phi gas thuc te |
|---|---|---|
| `PENDING` | Da tiep nhan yeu cau, dang gui/cho receipt | Chua |
| `SUCCESS` | Receipt thanh cong | Co |
| `FAILED_ON_CHAIN` | Giao dich da len chain nhung bi revert/that bai | Co |
| `SUBMISSION_FAILED` | Loi truoc khi giao dich duoc chap nhan boi blockchain | Khong |
| `RECEIPT_UNKNOWN` | Da co tx hash hoac da gui, nhung tam thoi chua lay duoc receipt | Chua, cho doi soat |

Tong phi gas thuc te:

```text
SUM(fee_wei) WHERE status IN ('SUCCESS', 'FAILED_ON_CHAIN')
```

Khong duoc hien thi `SUBMISSION_FAILED` la da ton gas neu khong co receipt chung minh.

## 3. Kien truc de xuat

### 3.1 Noi so huu du lieu gas

Them PostgreSQL rieng cho `blockchain-service` va de service nay so huu bang gas ledger.

Ly do:

- `blockchain-service` la noi truc tiep nhan `TransactionReceipt`.
- Service nay biet chinh xac `gasUsed`, gas price, tx hash, block number va trang thai on-chain.
- Tranh de cac service nghiep vu tu uoc luong gas.
- De xu ly retry Kafka va doi soat receipt tap trung.

Luong xu ly:

```text
Business service
  -> tao blockchain event kem billingActor
  -> Kafka
  -> blockchain-service ghi PENDING
  -> gui giao dich blockchain
  -> nhan receipt/exception
  -> cap nhat gas ledger
  -> gui reply ve service nghiep vu
```

### 3.2 Dinh danh idempotency

Moi yeu cau ghi blockchain phai co `requestId` on dinh va duy nhat.

Vi du:

```text
raw-batch:{rawBatchId}:record
pallet:{palletId}:anchor
trade-order:{orderId}:line:{lineId}:ownership
transfer:{transferId}:ownership
```

Kafka retry phai dung lai cung `requestId`. Khong tao UUID moi trong moi lan retry.

`requestId` la khoa chong ghi trung chinh. `txHash` la khoa chong trung thu hai sau khi giao dich da duoc gui.

## 4. Mo hinh du lieu

### 4.1 Bang `blockchain_gas_usage`

De xuat migration:

```sql
CREATE TABLE blockchain_gas_usage (
    id UUID PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    tx_hash VARCHAR(100),

    operation VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    source_service VARCHAR(100) NOT NULL,

    billing_actor_id UUID NOT NULL,
    billing_role VARCHAR(30) NOT NULL,
    initiated_by_user_id UUID,

    status VARCHAR(30) NOT NULL,
    gas_used NUMERIC(78, 0),
    effective_gas_price_wei NUMERIC(78, 0),
    fee_wei NUMERIC(78, 0),
    block_number NUMERIC(78, 0),

    error_code VARCHAR(100),
    error_message TEXT,

    submitted_at TIMESTAMP WITH TIME ZONE,
    mined_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_blockchain_gas_usage_request_id UNIQUE (request_id),
    CONSTRAINT ck_blockchain_gas_usage_billing_role
        CHECK (billing_role IN ('SUPPLIER', 'MANUFACTURER')),
    CONSTRAINT ck_blockchain_gas_usage_status
        CHECK (status IN (
            'PENDING',
            'SUCCESS',
            'FAILED_ON_CHAIN',
            'SUBMISSION_FAILED',
            'RECEIPT_UNKNOWN'
        ))
);

CREATE UNIQUE INDEX uk_blockchain_gas_usage_tx_hash
    ON blockchain_gas_usage(tx_hash)
    WHERE tx_hash IS NOT NULL;

CREATE INDEX idx_gas_usage_actor_created
    ON blockchain_gas_usage(billing_actor_id, created_at DESC);

CREATE INDEX idx_gas_usage_role_created
    ON blockchain_gas_usage(billing_role, created_at DESC);

CREATE INDEX idx_gas_usage_operation_created
    ON blockchain_gas_usage(operation, created_at DESC);

CREATE INDEX idx_gas_usage_status_updated
    ON blockchain_gas_usage(status, updated_at);
```

### 4.2 Operation duoc thong ke

```text
RECORD_BATCH
RECORD_TRANSFORMED_BATCH
OWNERSHIP_CHANGE
```

Khong ghi nhan cac operation chi doc nhu:

```text
VERIFY_BATCH
GET_BATCH
VERIFY_OWNERSHIP
```

### 4.3 Du lieu hien thi

API co the tra `feeWei` duoi dang chuoi de tranh tran so:

```json
{
  "feeWei": "1283400000000000",
  "gasUsed": "64170",
  "effectiveGasPriceWei": "20000000000"
}
```

Frontend tu quy doi Wei sang ETH de hien thi. Neu sau nay hien thi VND, can luu them ty gia tai thoi diem giao dich; khong dung ty gia hien tai de tinh lai lich su.

## 5. Thay doi Kafka event contract

### 5.1 Metadata bat buoc cho moi blockchain write event

Bo sung:

```text
requestId
operation
billingActorId
billingRole
initiatedByUserId
sourceService
```

Y nghia:

- `billingActorId`: nguoi/organization duoc phan bo chi phi.
- `billingRole`: snapshot role tai thoi diem tao giao dich.
- `initiatedByUserId`: nguoi thuc hien thao tac; co the la transporter.
- `billingActorId` va `initiatedByUserId` co the khac nhau.

Vi du transporter xac nhan giao hang:

```json
{
  "operation": "OWNERSHIP_CHANGE",
  "billingActorId": "seller-id",
  "billingRole": "MANUFACTURER",
  "initiatedByUserId": "transporter-id"
}
```

### 5.2 Reply event

Bo sung vao blockchain reply:

```text
requestId
operation
billingActorId
billingRole
txHash
status
gasUsed
effectiveGasPriceWei
feeWei
blockNumber
errorCode
errorMessage
```

Service nghiep vu khong dung reply de tinh phi; reply chi de cap nhat trang thai nghiep vu va ho tro quan sat.

### 5.3 Quy tac xac dinh nguoi chiu phi

| Nghiep vu | Billing actor |
|---|---|
| Supplier ghi raw batch | `producerId`/supplier tao lo |
| Manufacturer ghi pallet/transformed batch | manufacturer so huu pallet |
| Trade ownership change | `order.sellerId` |
| Generic transfer ownership change | `transfer.fromUserId` |

Khong suy ra nguoi chiu phi tu:

- HTTP caller hien tai.
- Kafka consumer.
- Nguoi van chuyen bam xac nhan.
- Dia chi system wallet.

Neu `billingRole` khong phai `SUPPLIER` hoac `MANUFACTURER`, blockchain-service phai tu choi/quarantine event va khong tao khoan phi cho actor do.

## 6. Xu ly giao dich blockchain

### 6.1 Chuan hoa ket qua thuc thi

Thay vi cac ham blockchain write chi tra `txHash`, tao model noi bo:

```text
BlockchainExecutionResult
- txHash
- status
- gasUsed
- effectiveGasPriceWei
- feeWei
- blockNumber
- submittedAt
- minedAt
- errorCode
- errorMessage
```

Ap dung cho:

- `recordBatch`
- `recordTransformedBatch`
- `logOwnershipChange`

### 6.2 Luong xu ly an toan

1. Nhan Kafka event.
2. Validate `requestId`, operation, billing actor va billing role.
3. Tim ledger theo `requestId`.
4. Neu da terminal (`SUCCESS`, `FAILED_ON_CHAIN`, `SUBMISSION_FAILED`) thi khong gui lai.
5. Tao ledger `PENDING` truoc khi submit.
6. Gui giao dich.
7. Neu co receipt, lay thong tin va cap nhat `SUCCESS` hoac `FAILED_ON_CHAIN`.
8. Neu loi truoc submit, cap nhat `SUBMISSION_FAILED`.
9. Neu timeout/khong chac receipt, cap nhat `RECEIPT_UNKNOWN`.
10. Gui reply Kafka.

Web3j co the nem exception trong khi exception van chua transaction receipt. Phan catch phai thu lay receipt tu exception truoc khi ket luan la `SUBMISSION_FAILED`.

### 6.3 Lay effective gas price

Thu tu uu tien:

1. Lay effective gas price tu receipt neu chain/client ho tro.
2. Neu receipt khong co, truy van transaction theo tx hash va lay gas price.
3. Chi fallback ve configured gas price khi chac chan giao dich dung gia do.

Neu khong xac dinh duoc gas price, khong duoc tu uoc luong phi. Giu ban ghi de doi soat.

## 7. Doi soat giao dich chua ro ket qua

Them scheduled job trong `blockchain-service`:

```text
GasUsageReconciliationJob
```

Job xu ly cac ban ghi:

- `PENDING` qua thoi gian cho phep.
- `RECEIPT_UNKNOWN`.

Quy trinh:

1. Tim receipt theo `txHash`.
2. Neu co receipt thanh cong: cap nhat `SUCCESS`.
3. Neu co receipt that bai: cap nhat `FAILED_ON_CHAIN` va tinh phi.
4. Neu chua co receipt: giu `RECEIPT_UNKNOWN`, tang retry count/ghi metric.
5. Khong bao gio tao them ledger moi khi doi soat.

Can co gioi han retry va canh bao cho ban ghi bi tre qua lau.

## 8. API thong ke

### 8.1 API cho supplier/manufacturer

```http
GET /api/v1/gas-usage/my/summary?from=&to=
GET /api/v1/gas-usage/my/transactions?page=&size=&operation=&status=&from=&to=
```

`/my` lay actor tu access token, khong nhan `actorId` tu client.

Summary de xuat:

```json
{
  "actualFeeWei": "1000000",
  "successFeeWei": "800000",
  "failedOnChainFeeWei": "200000",
  "successCount": 8,
  "failedOnChainCount": 1,
  "submissionFailedCount": 2,
  "receiptUnknownCount": 0,
  "operationBreakdown": [],
  "dailyBreakdown": []
}
```

### 8.2 API cho admin

```http
GET /api/v1/gas-usage/admin/summary?from=&to=&role=
GET /api/v1/gas-usage/admin/actors/{actorId}/summary?from=&to=
GET /api/v1/gas-usage/admin/transactions?page=&size=&role=&operation=&status=&from=&to=
```

### 8.3 Bao mat

- `SUPPLIER` va `MANUFACTURER` chi xem duoc thong ke cua chinh minh.
- `TRANSPORTER`, `RETAILER`, `USER` khong co menu va bi API tra `403`.
- `ADMIN` duoc xem tong hop.
- API ghi gas ledger khong duoc public.
- Khong tra private key, raw signed transaction hoac stack trace cho frontend.

## 9. Giao dien web

Them muc `Chi phi blockchain` cho supplier va manufacturer.

### 9.1 Tong quan

Hien thi:

- Tong phi gas thuc te da ton.
- Phi giao dich thanh cong.
- Phi giao dich that bai tren chain.
- So giao dich thanh cong.
- So giao dich that bai.
- So giao dich dang cho doi soat.

Can ghi ro:

```text
Chi phi blockchain duoc phan bo
```

Khong nen ghi nhu the he thong da tru tien tu vi cua actor, vi hien tai system wallet la vi thanh toan that.

### 9.2 Bieu do

- Phi gas theo ngay/thang.
- Phi gas theo operation.
- So luong giao dich theo trang thai.
- So sanh phi thanh cong va phi that bai tren chain.

### 9.3 Bang chi tiet

Cot de xuat:

```text
Thoi gian
Nghiep vu
Doi tuong/ma don
Trang thai
Gas used
Gas price
Phi gas
Tx hash
```

Trang thai that bai phai duoc hien thi ro:

- `That bai tren blockchain - co ton gas`
- `Khong gui duoc - khong ghi nhan phi`
- `Dang doi soat`

Khong tao trang thong ke gas cho transporter.

## 10. Cac module va vi tri can sua

### 10.1 `blockchain-service`

- Them PostgreSQL, JPA va Flyway.
- Them entity/repository/migration cho `blockchain_gas_usage`.
- Mo rong blockchain event DTO va reply DTO.
- Sua `TraceabilityServiceImpl` de tra ket qua tu receipt thay vi chi tx hash.
- Them service ghi ledger va tinh `feeWei`.
- Them idempotency theo `requestId`.
- Them reconciliation scheduled job.
- Them API summary/transaction va security.
- Them metric/log theo requestId va txHash.

### 10.2 `traceability-core-service`

- Khi tao/merge raw batch: gan supplier/producer lam `billingActor`.
- Khi anchor pallet/transformed batch: gan manufacturer lam `billingActor`.
- Tao `requestId` on dinh cho moi operation.
- Mo rong event/reply DTO tuong ung.

### 10.3 `trade-logistics-service`

- Ownership change cua trade order: luon dung `order.sellerId` lam `billingActorId`.
- Nguoi bam xac nhan giao hang chi duoc ghi vao `initiatedByUserId`.
- Generic transfer: dung `transfer.fromUserId`.
- Thay entity/request ID tam thoi bang ID on dinh theo order va order line.
- Mo rong event/reply DTO tuong ung.

### 10.4 Identity/role lookup

Can co mot cach dang tin cay de lay role cua nguoi ban:

- Uu tien luu `sellerRole` snapshot khi tao order/transfer; hoac
- Goi internal identity API khi tao event.

Khong lookup role tai blockchain-service sau khi giao dich da gui, vi role co the da thay doi.

### 10.5 Frontend web

- Them model, API client va state management cho gas usage.
- Them menu va dashboard cho supplier/manufacturer.
- Them filter thoi gian, operation, status va pagination.
- An menu voi cac role khong duoc phep.

## 11. Thu tu trien khai

### Giai doan 0: Chot quy tac va baseline

- Chot ten operation va status.
- Chot mui gio thong ke: `Asia/Ho_Chi_Minh`.
- Chot don vi hien thi mac dinh: Wei va ETH.
- Ghi lai gas config va luong Kafka hien tai.
- Chot ngay bat dau thong ke.

### Giai doan 1: Database cho blockchain-service

- Them dependency PostgreSQL/JPA/Flyway.
- Them datasource config qua environment variable.
- Tao migration `blockchain_gas_usage`.
- Tao entity/repository.
- Kiem tra index va constraint.

### Giai doan 2: Nang cap event contract

- Them metadata billing va `requestId` vao event.
- Them ket qua gas vao reply.
- Cap nhat moi ban sao DTO trong cac service.
- Trien khai backward-compatible: field moi nullable trong giai doan chuyen tiep hoac dung topic version moi.

### Giai doan 3: Ghi ledger va tinh phi

- Tao ledger `PENDING` truoc submit.
- Lay receipt va tinh phi bang `BigInteger`.
- Xu ly day du `SUCCESS`, `FAILED_ON_CHAIN`, `SUBMISSION_FAILED`, `RECEIPT_UNKNOWN`.
- Dam bao retry Kafka khong tao giao dich/ledger trung.

### Giai doan 4: Gan dung nguoi chiu phi

- Raw batch -> supplier.
- Pallet/transformed batch -> manufacturer.
- Ownership change -> seller.
- Transporter trigger -> seller van chiu phi.
- Chan role ngoai supplier/manufacturer.

### Giai doan 5: Doi soat

- Them scheduled reconciliation.
- Them retry policy va metric.
- Kiem tra truong hop service restart sau khi submit nhung truoc khi cap nhat ledger.

### Giai doan 6: API va bao mat

- Them API `/my`.
- Them API admin.
- Them authorization va kiem thu truy cap cheo actor.
- Them pagination/filter.

### Giai doan 7: Giao dien

- Dashboard tong quan.
- Bieu do.
- Bang chi tiet.
- Trang thai thanh cong/that bai/doi soat ro rang.
- Khong hien thi menu cho transporter.

### Giai doan 8: Rollout

Thu tu deploy:

1. Database va blockchain-service consumer ho tro event moi.
2. Cac producer nghiep vu gui metadata moi.
3. API thong ke.
4. Frontend.
5. Bat validate bat buoc cho metadata moi sau khi tat ca producer da nang cap.

### Giai doan 9: Theo doi sau rollout

- Theo doi so ban ghi `PENDING`/`RECEIPT_UNKNOWN`.
- Theo doi duplicate `requestId`.
- Canh bao loi doi soat receipt.
- Doi chieu `feeWei` trong DB voi receipt blockchain.
- Kiem tra query thong ke khi du lieu tang lon.

## 12. Ke hoach kiem thu

### 12.1 Unit test

- Tinh `feeWei` chinh xac voi so `BigInteger` rat lon.
- Receipt thanh cong tao `SUCCESS`.
- Receipt revert tao `FAILED_ON_CHAIN` va van co phi.
- Loi truoc submit tao `SUBMISSION_FAILED`, khong co phi.
- Timeout tao `RECEIPT_UNKNOWN`.
- Khong ghi gas cho operation chi doc.
- Khong chap nhan billing role transporter.

### 12.2 Integration test voi blockchain/Ganache

- Gui giao dich thanh cong va doi chieu ledger voi receipt.
- Tao giao dich revert va xac nhan phi that bai duoc ghi.
- Mo phong RPC down truoc submit va xac nhan khong tinh phi.
- Mo phong timeout sau submit, sau do reconciliation cap nhat dung.
- Restart blockchain-service giua submit va receipt, khong mat/nhan doi giao dich.

### 12.3 Kafka/idempotency test

- Gui lai cung event va `requestId`: chi co mot ledger.
- Consumer retry: khong gui lai giao dich neu da co ket qua terminal.
- Duplicate tx hash: bi unique constraint chan.

### 12.4 Business rule test

- Supplier ghi raw batch: supplier bi tinh phi.
- Manufacturer ghi pallet: manufacturer bi tinh phi.
- Supplier ban cho manufacturer: supplier chiu phi ownership change.
- Manufacturer ban cho manufacturer: manufacturer nguoi ban chiu phi.
- Transporter bam giao thanh cong: seller chiu phi, transporter khong co ledger.
- Verify blockchain: khong tao ledger.

### 12.5 API/security test

- Supplier/manufacturer chi xem duoc du lieu cua minh.
- Actor A khong xem duoc actor B.
- Transporter bi `403`.
- Admin xem duoc tong hop.
- Filter ngay, operation, status va pagination dung.
- Boundary ngay dung mui gio `Asia/Ho_Chi_Minh`.

### 12.6 Performance test

- Tao du lieu lon va chay summary theo actor/thoi gian.
- Kiem tra `EXPLAIN ANALYZE` su dung cac index da tao.
- Kiem tra pagination khong load toan bo ledger vao bo nho.

## 13. Tieu chi nghiem thu

Chuc nang duoc coi la hoan thanh khi:

- Moi blockchain write operation hop le deu co mot ledger duy nhat.
- Giao dich thanh cong ghi dung gas va phi tu receipt.
- Giao dich that bai tren chain van duoc tinh phi.
- Loi truoc khi len chain duoc thong ke that bai nhung khong bi tinh phi.
- Giao dich khong ro receipt duoc doi soat va khong bi tinh trung.
- Ownership change luon phan bo cho nguoi ban.
- Transporter khong co chi phi gas.
- Supplier/manufacturer chi xem duoc thong ke cua minh.
- Tong phi tren dashboard khop tong receipt blockchain trong tap kiem thu.
- Query thong ke su dung index va hoat dong on dinh voi du lieu lon.

## 14. Du lieu lich su va gioi han

- Cac giao dich cu co the truy nguoc gas tu tx hash, nhung khong chac xac dinh duoc billing actor.
- Khuyen nghi bat dau thong ke chinh thuc tu ngay rollout.
- Neu backfill, cac ban ghi khong xac dinh duoc actor phai danh dau la chi phi he thong/unknown va loai khoi thong ke theo actor.
- Tren Ganache, ETH va gas la gia tri mo phong; dashboard nen ghi ro de tranh hieu la chi phi tien that.

## 15. Viec nen lam kem khi trien khai

- Chuyen blockchain private key hien dang cau hinh truc tiep sang environment variable/secret.
- Khong ghi private key vao log.
- Them correlation log theo `requestId` va `txHash`.
- Them metric:
  - So giao dich theo status.
  - Tong gas/fee theo operation.
  - So giao dich cho doi soat.
  - So lan duplicate request.
  - So loi reconciliation.


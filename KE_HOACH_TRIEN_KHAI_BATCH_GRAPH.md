# Ke hoach trien khai truy xuat nguon goc theo Batch Graph

> Luu y: Day la phuong an kien truc tong quat cho huong phat trien dai han.
> Ke hoach trien khai chinh cho pham vi do an hien tai nam tai
> `KE_HOACH_TRUY_XUAT_TUNG_CAP_PALLET_INPUTS.md`.

## 1. Muc tieu

He thong hien tai truy xuat theo chuoi co dinh:

```text
RawBatch -> Pallet -> Carton -> ProductUnit
```

Mo hinh nay phu hop khi nha may chi su dung nguyen lieu tho, vi du sua bo
tu trang trai. Tuy nhien, no khong bieu dien tot truong hop nha may su dung
ban thanh pham hoac thanh pham cua nha may khac lam nguyen lieu dau vao.

Muc tieu moi la bieu dien chuoi cung ung duoi dang do thi lo truy xuat:

```text
TraceBatch -> TraceBatch -> ... -> Pallet -> Carton -> ProductUnit
```

Vi du:

```text
Lo sua bo tai trang trai
    -> Lo sua dac cua Nha may A
        -> Lo banh sua cua Nha may B
            -> Pallet -> Carton -> Unit
```

Moi lo la mot node `TraceBatch`. Quan he lo dau vao duoc dung de tao lo dau
ra la mot edge `BatchInput`.

---

## 2. Giai thich cac loai batch

### `RAW_MATERIAL`

La lo nguyen lieu goc, chua duoc tao ra tu mot lo dau vao nao khac trong he
thong.

Vi du:

- Sua bo vua thu hoach tai trang trai.
- Hat ca phe thu hoach tai nong trai.
- Mia duong thu hoach tai vung nguyen lieu.

Loai batch nay khong co `BatchInput` cha.

```text
Sua bo tuoi -> RAW_MATERIAL
```

### `SEMI_FINISHED`

La lo ban thanh pham da qua che bien, nhung tiep tuc duoc su dung lam dau
vao de san xuat mot san pham khac.

Vi du:

- Sua dac duoc tao tu sua bo.
- Bot cacao duoc tao tu hat cacao.
- Duong tinh luyen duoc tao tu mia.
- Phoi nhua duoc dung de san xuat chai nhua.

Mot san pham co phai `SEMI_FINISHED` hay khong phu thuoc vao vai tro cua no
trong chuoi san xuat, khong chi phu thuoc vao ten san pham.

```text
Sua bo tuoi -> Sua dac
RAW_MATERIAL   SEMI_FINISHED
```

### `FINISHED_PRODUCT`

La lo thanh pham cuoi cung cua mot quy trinh san xuat, duoc dong goi thanh
carton/unit de ban hoac phan phoi toi khach hang.

Vi du:

- Hop sua dac ban le.
- Banh sua dong goi.
- Chai nuoc giai khat.

```text
Sua bo -> Sua dac -> Banh sua dong goi
RAW       SEMI       FINISHED_PRODUCT
```

Mot lo `FINISHED_PRODUCT` van co the tro thanh dau vao cua nha may khac trong
tuong lai. Khi do, lo van giu lich su va loai ban dau, nhung trong giao dich
san xuat moi no dong vai tro la input batch. Neu nghiep vu thuong xuyen co
truong hop nay, co the doi ten `batchType` thanh `productionStage` de tranh
hieu rang thanh pham khong bao gio duoc su dung lai.

---

## 3. Nguyen tac kien truc

1. Tach lo san xuat/truy xuat khoi pallet vat ly.
2. Moi lo dau vao va dau ra deu la `TraceBatch`.
3. `BatchInput` ghi nhan input batch nao duoc dung cho output batch nao.
4. Mot output batch co the co nhieu input batch.
5. Mot input batch co the duoc dung mot phan cho nhieu output batch.
6. Khong cho phep tao chu trinh trong graph.
7. Du lieu nghiep vu day du nam trong PostgreSQL.
8. Blockchain luu hash bat bien de doi chieu tinh toan ven.
9. Trace thong thuong khong bat buoc goi blockchain.
10. Verify blockchain la thao tac rieng do nguoi dung yeu cau.

---

## 4. Mo hinh du lieu de xuat

### Bang `trace_batches`

```text
id UUID PK
batch_code VARCHAR UNIQUE
batch_type VARCHAR
item_type VARCHAR
item_id VARCHAR
item_name_snapshot VARCHAR

actor_id VARCHAR
owner_id VARCHAR
manufacturer_id VARCHAR

original_quantity DECIMAL
remaining_quantity DECIMAL
unit VARCHAR

manufactured_at TIMESTAMP
expiry_at TIMESTAMP
location VARCHAR
note TEXT
schema_version VARCHAR

chain_batch_id_hex VARCHAR UNIQUE
data_hash_hex VARCHAR
anchor_tx_hash VARCHAR
anchor_status VARCHAR

created_at TIMESTAMP
updated_at TIMESTAMP
version BIGINT
```

Gia tri `item_type`:

```text
MATERIAL
PRODUCT
```

Cot `version` dung optimistic locking de tranh hai giao dich cung su dung
mot input batch dan den tieu thu vuot ton kho.

### Bang `batch_inputs`

```text
id UUID PK
output_batch_id UUID FK -> trace_batches.id
input_batch_id UUID FK -> trace_batches.id
quantity_used DECIMAL
unit VARCHAR
created_at TIMESTAMP
```

Rang buoc:

```text
UNIQUE(output_batch_id, input_batch_id)
CHECK(output_batch_id <> input_batch_id)
CHECK(quantity_used > 0)
```

Index:

```sql
CREATE INDEX idx_batch_inputs_output
    ON batch_inputs(output_batch_id);

CREATE INDEX idx_batch_inputs_input
    ON batch_inputs(input_batch_id);

CREATE INDEX idx_trace_batches_owner_type
    ON trace_batches(owner_id, batch_type);

CREATE INDEX idx_trace_batches_item
    ON trace_batches(item_type, item_id);

CREATE UNIQUE INDEX uk_trace_batches_chain_id
    ON trace_batches(chain_batch_id_hex);
```

### Lien ket dong goi

Them vao `pallets`:

```text
trace_batch_id UUID FK -> trace_batches.id
```

Quan he sau khi thay doi:

```text
TraceBatch thanh pham
    -> Pallet vat ly
        -> Carton
            -> ProductUnit
```

Mot lo san xuat co the duoc chia thanh nhieu pallet vat ly.

---

## 5. Quy tac nghiep vu can chot

1. Input batch chi duoc su dung khi user co quyen so huu hoac quyen su dung.
2. Input batch co the duoc su dung mot phan.
3. Khong duoc dung vuot `remaining_quantity`.
4. Khong duoc tao quan he lam xuat hien cycle.
5. Don vi dau vao phai tuong thich hoac co quy tac quy doi ro rang.
6. Sau khi batch da neo blockchain, cac field tham gia tinh hash khong duoc
   sua truc tiep.
7. Batch khong bi xoa khoi lich su; khi huy chi thay doi trang thai.
8. Chuyen giao batch khong thay doi manufacturer ban dau.
9. Moi giao dich tieu thu input va tao output phai nam trong mot DB
   transaction.

---

## 6. Giai doan trien khai

### Giai doan 1: Them schema moi

Tao cac Flyway migration:

```text
V8__create_trace_batches.sql
V9__create_batch_inputs.sql
V10__link_pallets_to_trace_batches.sql
V11__backfill_existing_trace_batches.sql
```

Chua xoa:

```text
raw_batches
pallets.parent_raw_batch_id_hexes
```

He thong cu va moi cung ton tai trong giai doan chuyen doi.

### Giai doan 2: Backfill du lieu hien tai

- Moi `RawBatch` cu tao mot `TraceBatch` loai `RAW_MATERIAL`.
- Moi `Pallet` cu tao mot `TraceBatch` loai `FINISHED_PRODUCT`.
- Chuyen `parentRawBatchIdHexes` thanh cac ban ghi `BatchInput`.
- Gan `pallet.trace_batch_id` vao batch thanh pham tuong ung.
- Giu nguyen `chainBatchIdHex`, `dataHashHex`, `anchorTxHash`.

Kiem tra sau migration:

```text
So RawBatch = so TraceBatch RAW duoc migrate
So Pallet = so TraceBatch FINISHED duoc migrate
Moi parentRawBatchIdHex co BatchInput tuong ung
Khong co BatchInput mo coi
Khong co cycle
```

### Giai doan 3: Them domain va repository

Them vao `traceability-core-service`:

```text
entity/TraceBatch.java
entity/BatchInput.java
repository/TraceBatchRepository.java
repository/BatchInputRepository.java
service/TraceBatchService.java
service/BatchGraphService.java
controller/TraceBatchController.java
```

### Giai doan 4: API batch moi

```http
POST /api/v1/trace-batches/raw
POST /api/v1/trace-batches/transformed
GET  /api/v1/trace-batches/{id}
GET  /api/v1/trace-batches/my
GET  /api/v1/trace-batches/available-inputs
GET  /api/v1/trace-batches/{id}/graph
```

Vi du request tao lo sua dac:

```json
{
  "batchType": "SEMI_FINISHED",
  "itemType": "PRODUCT",
  "itemId": "condensed-milk-product-id",
  "quantity": 1000,
  "unit": "KG",
  "manufacturedAt": "2026-06-04T08:00:00",
  "inputs": [
    {
      "batchId": "raw-milk-batch-id",
      "quantityUsed": 1500,
      "unit": "LITER"
    }
  ]
}
```

Khi tao batch, service phai:

1. Khoa hoac optimistic-lock cac input batch.
2. Kiem tra ownership.
3. Kiem tra trang thai batch.
4. Kiem tra so luong con lai.
5. Kiem tra don vi.
6. Kiem tra cycle.
7. Tao output batch va cac `BatchInput`.
8. Tru `remaining_quantity`.
9. Phat Kafka event neo blockchain.

### Giai doan 5: Xay dung `BatchGraphService`

Chuc nang:

```text
getAncestors(batchId)
getDescendants(batchId)
detectCycle(outputBatchId, inputBatchIds)
buildTraceGraph(batchId)
flattenGraphToTimeline(batchId)
```

Yeu cau ky thuat:

- Dung `visitedBatchIds` de tranh lap.
- Gioi han do sau phong du lieu loi.
- Batch-load node va edge de tranh N+1 query.
- Bao loi ro rang khi phat hien cycle.

### Giai doan 6: Blockchain va hash

Smart contract hien tai da co:

```solidity
recordBatch(...)
recordTransformedBatch(..., parentHashes)
```

Phien ban dau chua can thay contract. Backend can:

- Chuan hoa cach tinh hash cho `TraceBatch`.
- Sap xep input on dinh truoc khi tinh hash.
- Dua quan he input va quantity vao payload hash.
- Luu `anchorStatus`: `PENDING`, `CONFIRMED`, `FAILED`.
- Verify moi node trong graph.

Trang thai verify:

```text
VERIFIED
MISMATCH
NOT_ANCHORED
VERIFY_FAILED
```

### Giai doan 7: Sua trace theo unit

Thay logic hard-code:

```text
Unit -> Carton -> Pallet -> RawBatch
```

bang:

```text
Unit
    -> Carton
        -> Pallet
            -> TraceBatch
                -> BatchGraphService.buildTraceGraph(...)
```

Giu cac API hien tai de mobile cu tiep tuc hoat dong:

```http
GET /product/api/v1/units/trace/by-serial
GET /product/api/v1/units/trace/by-serial/verify
```

Response moi bo sung `batchGraph`, trong khi tam thoi van tra
`historyEvents`.

### Giai doan 8: Trade va chuyen quyen so huu

Chuyen target chinh sang:

```text
TRACE_BATCH
CARTON
UNIT
```

Don hang giua hai nha may cho phep ban/chuyen:

```text
RAW_MATERIAL
SEMI_FINISHED
FINISHED_PRODUCT
```

Sau giao hang thanh cong:

- Chuyen `ownerId`.
- Khong thay doi `manufacturerId`.
- Khong thay doi graph nguon goc.
- Ghi ownership event len blockchain.
- Batch xuat hien trong danh sach input cua nha may nhan.

### Giai doan 9: Frontend web

Tao man `Quan ly lo truy xuat` voi cac tab:

```text
Nguyen lieu tho
Ban thanh pham
Thanh pham
Da su dung het
```

Man tao lo san xuat cho phep chon nhieu input batch va nhap so luong su dung
cho tung batch.

Man chi tiet batch hien thi:

- Thong tin batch.
- Input truc tiep.
- Output da su dung batch nay.
- Cay nguon goc.
- Lich su chuyen giao.
- Trang thai blockchain.

### Giai doan 10: Mobile

Phan scan QR va tim `unitSerial` khong can thay doi.

Sua man ket qua trace de:

- Hien thi thong tin san pham.
- Hien thi cay nguon goc nhieu tang.
- Mo chi tiet tung batch.
- Hien thi nha san xuat, vi tri va so luong da su dung.
- Hien thi verify rieng cho tung batch.

### Giai doan 11: Hieu nang

- Tao index cho `batch_inputs`.
- Batch-load graph thay vi query tung node.
- Cache thong tin catalog va actor.
- Gom API lay transfer cho nhieu target.
- Khong verify blockchain trong trace thong thuong.
- Cache graph theo `rootBatchId`.
- Xoa cache khi batch hoac transfer thay doi.

Neu graph rat lon, co the bo sung closure table sau:

```text
batch_ancestry
ancestor_batch_id
descendant_batch_id
depth
```

### Giai doan 12: Kiem thu

Unit test:

- Tao raw batch khong co input.
- Tao transformed batch tu raw batch.
- Tao transformed batch tu semi-finished batch.
- Mot output dung nhieu input.
- Mot input dung cho nhieu output.
- Chan su dung vuot quantity.
- Chan su dung batch khong so huu.
- Chan cycle.
- Hash on dinh bat ke thu tu input request.

Integration test chinh:

```text
Sua bo
    -> Sua dac tai Nha may A
        -> Banh sua tai Nha may B
            -> Pallet
                -> Carton
                    -> Unit
                        -> Scan
                            -> Verify
```

Migration test:

- Clone DB hien tai.
- Chay Flyway migration.
- So sanh trace response truoc va sau.
- Dam bao moi unit cu van truy xuat duoc.

---

## 7. Thong tin khach hang nhan duoc khi truy xuat

Khi khach hang quet QR/serial cua mot unit, he thong se:

```text
unitSerial
    -> ProductUnit
        -> Carton
            -> Pallet
                -> TraceBatch thanh pham
                    -> toan bo graph batch nguon
```

Khach hang nen nhan duoc ba nhom thong tin.

### 7.1. Thong tin san pham cuoi

- Ten va hinh anh san pham.
- Mo ta san pham.
- Unit serial.
- Ma carton.
- Ma pallet.
- Ma lo san xuat.
- Ngay san xuat.
- Han su dung.
- Nha san xuat cuoi.
- So luot truy xuat.

### 7.2. Cay nguon goc

Vi du khach hang quet mot hop banh sua:

```text
Banh sua - Lo CAKE-20260604
San xuat boi Nha may B, ngay 04/06/2026
|
+-- Sua dac - Lo CONDENSED-20260601
|   San xuat boi Nha may A, ngay 01/06/2026
|   |
|   +-- Sua bo tuoi - Lo MILK-FARM-20260531
|       Cung cap boi Trang trai X, ngay 31/05/2026
|
+-- Duong - Lo SUGAR-20260528
    Cung cap boi Nha cung cap Y, ngay 28/05/2026
```

Voi moi batch, khach hang co the xem:

- Loai batch.
- Ma lo.
- Ten nguyen lieu/san pham.
- Nha cung cap hoac nha san xuat.
- Dia diem.
- Ngay tao/thu hoach/san xuat.
- So luong dau vao da su dung neu duoc phep cong khai.
- Lich su chuyen giao.
- Trang thai xac minh blockchain.

### 7.3. Ket qua xac minh

Trace thong thuong:

```text
Chua thuc hien xac minh blockchain
```

Khi khach hang bam `Xac minh nguon goc`, he thong hien thi:

```text
Toan bo du lieu nguyen ven
```

hoac chi ro batch co van de:

```text
Lo sua bo MILK-FARM-20260531: VERIFIED
Lo sua dac CONDENSED-20260601: MISMATCH
Lo banh sua CAKE-20260604: VERIFIED
```

Khong nen chi tra mot co `isDataIntact` chung. Can tra trang thai tung batch
de khach hang biet mat xich nao co sai lech.

---

## 8. Mau response trace de xuat

```json
{
  "unit": {
    "unitId": "unit-id",
    "unitSerial": "CD0437162",
    "cartonCode": "CARTON-001",
    "palletCode": "PALLET-001",
    "scanCount": 42
  },
  "product": {
    "id": "product-id",
    "name": "Banh sua",
    "description": "Banh sua dong goi",
    "imageUrl": "https://example.com/product.jpg",
    "manufacturedAt": "2026-06-04T08:00:00",
    "expiryAt": "2026-12-04T08:00:00"
  },
  "traceSummary": {
    "totalBatches": 4,
    "maximumDepth": 3,
    "verificationStatus": "NOT_VERIFIED"
  },
  "batchGraph": {
    "rootBatchId": "cake-batch-id",
    "nodes": [
      {
        "batchId": "cake-batch-id",
        "batchCode": "CAKE-20260604",
        "batchType": "FINISHED_PRODUCT",
        "itemName": "Banh sua",
        "actorName": "Nha may B",
        "location": "Ha Noi",
        "manufacturedAt": "2026-06-04T08:00:00",
        "verificationStatus": "NOT_VERIFIED"
      },
      {
        "batchId": "condensed-milk-batch-id",
        "batchCode": "CONDENSED-20260601",
        "batchType": "SEMI_FINISHED",
        "itemName": "Sua dac",
        "actorName": "Nha may A",
        "location": "Moc Chau",
        "manufacturedAt": "2026-06-01T08:00:00",
        "verificationStatus": "NOT_VERIFIED"
      },
      {
        "batchId": "raw-milk-batch-id",
        "batchCode": "MILK-FARM-20260531",
        "batchType": "RAW_MATERIAL",
        "itemName": "Sua bo tuoi",
        "actorName": "Trang trai X",
        "location": "Moc Chau",
        "manufacturedAt": "2026-05-31T06:00:00",
        "verificationStatus": "NOT_VERIFIED"
      }
    ],
    "edges": [
      {
        "inputBatchId": "condensed-milk-batch-id",
        "outputBatchId": "cake-batch-id",
        "quantityUsed": 500,
        "unit": "KG"
      },
      {
        "inputBatchId": "raw-milk-batch-id",
        "outputBatchId": "condensed-milk-batch-id",
        "quantityUsed": 1500,
        "unit": "LITER"
      }
    ]
  }
}
```

---

## 9. Thu tu rollout an toan

1. Deploy migration va entity moi.
2. Backfill du lieu cu.
3. Deploy dual-write: tao du lieu cu dong thoi tao `TraceBatch`.
4. Doi chieu du lieu dual-write.
5. Deploy API graph moi.
6. Chuyen frontend web sang API moi.
7. Chuyen mobile sang response graph.
8. Chuyen trade sang `TRACE_BATCH`.
9. Ngung ghi `parentRawBatchIdHexes`.
10. Sau mot giai doan on dinh moi xoa model cu.

Feature flag de xuat:

```text
trace.batch-graph.enabled
trace.batch-graph.dual-write-enabled
trace.batch-graph.read-from-new-model
```

---

## 10. Definition of Done

- Unit cu va moi deu truy xuat duoc.
- Nha may co the mua ban thanh pham cua nha may khac lam input.
- He thong truy nguoc duoc nhieu tang toi nguyen lieu goc.
- Khong the tao cycle hoac tieu thu vuot so luong.
- Moi batch trong graph co ket qua verify rieng.
- Trade chuyen duoc ownership cua `TraceBatch`.
- API cu van hoat dong trong thoi gian chuyen tiep.
- Migration chay duoc tren ban sao production ma khong mat du lieu.

Thu tu trien khai uu tien:

```text
TraceBatch + BatchInput schema
    -> Backfill du lieu cu
        -> BatchGraphService
            -> API trace graph
                -> Trade
                    -> Web/Mobile
```

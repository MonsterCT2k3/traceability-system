# Ke hoach trien khai truy xuat tung cap voi Pallet Inputs

## 1. Quyet dinh pham vi

Tai lieu nay la ke hoach trien khai chinh cho pham vi do an tot nghiep.

He thong se giu mo hinh hien tai:

```text
RawBatch -> Pallet -> Carton -> ProductUnit
```

Va mo rong de `Pallet` co the duoc tao tu:

```text
- RawBatch
- Pallet cua nha san xuat khac
```

Vi du:

```text
RawBatch sua bo cua Trang trai X
    -> Pallet sua dac cua Nha may A
        -> Pallet banh sua cua Nha may B
            -> Carton
                -> ProductUnit
```

Khach hang truy xuat theo tung cap:

1. Quet QR cua `ProductUnit`.
2. Xem va xac minh pallet thanh pham cung cac input truc tiep.
3. Neu mot input la pallet co nguon goc sau hon, khach hang bam vao pallet do.
4. He thong tai va xac minh tiep pallet dang xem cung input truc tiep cua no.

He thong khong tu dong tai hoac verify toan bo chuoi nguon goc trong mot request.

---

## 2. Muc tieu

### Muc tieu nghiep vu

- Cho phep nha san xuat dat mua pallet tu nha san xuat khac.
- Cho phep pallet cua nha san xuat khac tro thanh input san xuat.
- Luu duoc quan he truc tiep giua pallet dau ra va cac input.
- Khach hang co the xem sau tung tang khi co nhu cau.
- Ho tro truy xuat nguoc day du khi dieu tra, nhung khong tai het mac dinh.

### Muc tieu ky thuat

- Khong thay doi smart contract hien tai.
- Giu tuong thich voi du lieu RawBatch va Pallet hien co.
- Giu cac API trace theo `unitSerial`.
- Tranh response qua lon va tranh verify blockchain khong can thiet.
- Tranh quan he vong lap giua cac pallet.

---

## 3. Gioi han duoc chap nhan trong do an

1. `Pallet` dai dien cho mot lo san xuat co the chuyen giao.
2. Nha san xuat mua va su dung nguyen pallet lam input.
3. Chua ho tro su dung mot phan pallet.
4. Mot pallet input chi duoc tieu thu mot lan.
5. Carton va ProductUnit khong duoc chon truc tiep lam input san xuat.
6. Khong ho tro chia tach, tron lai hoac quy doi so luong phuc tap.
7. Khong tu dong verify de quy toan bo chuoi.
8. Trace va verify sau hon duoc thuc hien khi nguoi dung bam vao input pallet.

Nhung gioi han nay can duoc ghi ro trong bao cao va muc huong phat trien.

---

## 4. Mo hinh du lieu

### 4.1. Bang moi `pallet_inputs`

Bang nay luu quan he input truc tiep cua moi pallet dau ra.

```text
id UUID PK
output_pallet_id UUID FK -> pallets.id
input_type VARCHAR NOT NULL
input_id UUID NOT NULL
input_batch_id_hex VARCHAR(66) NOT NULL
created_at TIMESTAMP NOT NULL
```

Gia tri `input_type`:

```text
RAW_BATCH
PALLET
```

Y nghia:

```text
output_pallet_id:
    pallet duoc san xuat

input_type + input_id:
    RawBatch hoac Pallet duoc su dung truc tiep

input_batch_id_hex:
    batch ID tren blockchain cua input
```

Neu input la RawBatch:

```text
input_batch_id_hex = raw_batches.batch_id_hex
```

Neu input la Pallet:

```text
input_batch_id_hex = pallets.chain_batch_id_hex
```

### 4.2. Rang buoc va index

```sql
ALTER TABLE pallet_inputs
    ADD CONSTRAINT uk_pallet_inputs_output_type_input
    UNIQUE (output_pallet_id, input_type, input_id);

CREATE INDEX idx_pallet_inputs_output
    ON pallet_inputs(output_pallet_id);

CREATE INDEX idx_pallet_inputs_input
    ON pallet_inputs(input_type, input_id);
```

Backend phai chan:

```text
output pallet khong duoc dung chinh no lam input
khong duoc tao cycle pallet A -> B -> A
```

Khong the tao foreign key truc tiep cho `input_id` vi cot nay co the tro toi hai
bang khac nhau. Tinh hop le cua `input_id` duoc kiem tra trong service.

### 4.3. Them trang thai su dung vao `pallets`

Them:

```text
input_status VARCHAR NOT NULL DEFAULT 'AVAILABLE'
```

Gia tri:

```text
AVAILABLE   co the duoc dung lam input san xuat
RESERVED    dang nam trong don hang hoac quy trinh san xuat
CONSUMED    da duoc dung de tao pallet khac
```

Trang thai logistics hien tai va `input_status` la hai khai niem khac nhau.
Khong nen dung chung mot cot.

### 4.4. Them lien ket don hang

Them vao `trade_order_lines`:

```text
target_pallet_id VARCHAR(36)
```

Them order type:

```text
MANUFACTURER_TO_MANUFACTURER
```

Don nay mua chinh xac mot hoac nhieu pallet, khong chi mua theo `productId`.
Dieu nay dam bao he thong biet lo nao da duoc chuyen giao.

---

## 5. Flyway migration

### Core service

Tao migration:

```text
V8__create_pallet_inputs.sql
V9__add_pallet_input_status.sql
V10__backfill_pallet_inputs_from_parent_raw_batches.sql
```

Backfill:

1. Doc `pallets.parent_raw_batch_id_hexes`.
2. Tim RawBatch tuong ung theo `batch_id_hex`.
3. Tao mot `pallet_inputs` loai `RAW_BATCH` cho tung quan he.
4. Giu lai `parent_raw_batch_id_hexes` trong giai doan chuyen doi.

### Trade service

Tao migration:

```text
Vx__add_manufacturer_to_manufacturer_orders.sql
```

Migration them:

```text
trade_order_lines.target_pallet_id
```

Neu `order_type` co CHECK constraint trong DB thi cap nhat constraint de chap
nhan `MANUFACTURER_TO_MANUFACTURER`.

### Kiem tra sau migration

```text
- Moi Pallet cu co day du pallet_inputs RAW_BATCH.
- Khong co pallet_inputs mo coi.
- Khong co quan he pallet tu tro vao chinh no.
- Tat ca input_batch_id_hex dung dinh dang bytes32.
```

---

## 6. Backend Core: entity va repository

### Entity moi

```text
entity/PalletInput.java
domain/PalletInputType.java
domain/PalletInputStatus.java
```

`PalletInputType`:

```java
public enum PalletInputType {
    RAW_BATCH,
    PALLET
}
```

### Repository moi

```text
repository/PalletInputRepository.java
```

Method can co:

```java
List<PalletInput> findByOutputPalletIdOrderByCreatedAtAsc(String outputPalletId);

boolean existsByInputTypeAndInputId(
    PalletInputType inputType,
    String inputId
);

List<PalletInput> findByInputTypeAndInputId(
    PalletInputType inputType,
    String inputId
);
```

Them repository query de batch-load input, tranh query tung dong khi mot pallet
co nhieu input.

---

## 7. Backend Core: tao pallet tu input tong quat

### Request moi

Thay:

```json
{
  "parentRawBatchIdHexes": []
}
```

bang:

```json
{
  "palletName": "Banh sua lo 01",
  "batchNo": "CAKE-20260604",
  "manufacturedAt": "2026-06-04",
  "quantity": "1000",
  "unit": "BOX",
  "inputs": [
    {
      "type": "RAW_BATCH",
      "id": "raw-batch-id"
    },
    {
      "type": "PALLET",
      "id": "condensed-milk-pallet-id"
    }
  ]
}
```

Tao DTO:

```text
PalletInputRequest
- type
- id
```

Sua:

```text
PalletAnchorRequest
PalletServiceImpl.anchorPallet(...)
```

### Validation khi tao pallet

Trong mot transaction:

1. Xac dinh user dang dang nhap.
2. Kiem tra moi input ton tai.
3. Kiem tra user dang so huu moi input.
4. Neu input la pallet, kiem tra `input_status = AVAILABLE`.
5. Kiem tra input pallet chua bi dung trong output pallet khac.
6. Kiem tra khong tao cycle.
7. Lay blockchain batch ID cua tung input.
8. Sort danh sach batch ID theo thu tu on dinh.
9. Tao pallet dau ra.
10. Tao cac `PalletInput`.
11. Chuyen pallet input sang `CONSUMED`.
12. Phat Kafka event `blockchain.requests.transformed`.

### Kiem tra cycle

Khi tao output pallet moi, cycle gan nhu khong xay ra vi output chua ton tai.
Tuy nhien van can co validation cho cac API sua quan he hoac migration.

Ham de xuat:

```text
boolean wouldCreateCycle(outputPalletId, inputPalletId)
```

Ham di nguoc qua `pallet_inputs` loai `PALLET` va dung `visitedIds` de tranh
lap vo han.

---

## 8. Don hang nha san xuat toi nha san xuat

### Them order type

Sua `OrderType` tai trade service va cac DTO lien quan:

```text
MANUFACTURER_TO_MANUFACTURER
```

### Request mau

```json
{
  "orderType": "MANUFACTURER_TO_MANUFACTURER",
  "sellerId": "manufacturer-a-id",
  "lines": [
    {
      "targetPalletId": "condensed-milk-pallet-id"
    }
  ]
}
```

### Validation don hang

Khi tao don:

```text
- Buyer co role MANUFACTURER.
- Seller co role MANUFACTURER.
- Buyer va seller khac nhau.
- Pallet ton tai.
- Pallet thuoc seller.
- Pallet co input_status = AVAILABLE.
- Pallet khong nam trong don M2M dang xu ly khac.
```

Khi seller chap nhan:

```text
- Chuyen pallet sang RESERVED.
```

Khi tu choi hoac huy:

```text
- Chuyen pallet tu RESERVED ve AVAILABLE.
```

Khi giao thanh cong:

```text
- Chuyen pallet.ownerId sang buyer.
- Ghi ownership event len blockchain.
- Chuyen pallet ve AVAILABLE de buyer co the dung lam input.
```

### Quyen so huu carton va unit con

Can chot mot trong hai quy tac:

#### Quy tac de xuat cho do an

Chi pallet chua duoc giao cho retailer moi duoc ban qua M2M.

Khi giao M2M thanh cong:

```text
- Chuyen owner pallet.
- Chuyen owner cua carton va unit thuoc pallet sang buyer.
```

Dieu nay giu nhat quan ton kho vat ly.

Khong cho phep ban M2M neu mot phan carton/unit cua pallet da duoc giao cho
doi tac khac.

---

## 9. API truy xuat tung cap

### 9.1. Trace unit ban dau

Giu API:

```http
GET /product/api/v1/units/trace/by-serial?serial={serial}
```

Response tra:

```text
- Thong tin ProductUnit.
- Thong tin san pham.
- Thong tin pallet thanh pham.
- Danh sach input truc tiep cua pallet.
- hasInputs cho tung input.
- Trang thai verify neu da thuc hien.
```

Khong tu dong tai input cua input.

### 9.2. Mo sau mot pallet input

Them API:

```http
GET /product/api/v1/pallets/{palletId}/trace-direct
```

API nay tra cung cau truc:

```text
currentPallet + directInputs
```

Neu mot direct input la pallet va `hasInputs = true`, mobile cho phep bam tiep.

### 9.3. DTO de xuat

```text
DirectTraceResponse
- currentNode
- directInputs
- verificationScope
- verificationSummary
```

```text
TraceNodeResponse
- id
- nodeType: RAW_BATCH | PALLET
- code
- name
- actorId
- actorName
- location
- createdAt
- manufacturedAt
- blockchainBatchIdHex
- hasInputs
- verificationStatus
```

```text
DirectVerificationSummary
- currentNodeStatus
- inputRelationStatus
- verifiedInputCount
- totalInputCount
- overallStatus
```

### Response mau

```json
{
  "currentNode": {
    "id": "cake-pallet-id",
    "nodeType": "PALLET",
    "code": "PALLET-CAKE-001",
    "name": "Banh sua",
    "actorName": "Nha may B",
    "hasInputs": true,
    "verificationStatus": "VERIFIED"
  },
  "directInputs": [
    {
      "id": "condensed-milk-pallet-id",
      "nodeType": "PALLET",
      "code": "PALLET-MILK-001",
      "name": "Sua dac",
      "actorName": "Nha may A",
      "hasInputs": true,
      "verificationStatus": "VERIFIED"
    },
    {
      "id": "sugar-raw-batch-id",
      "nodeType": "RAW_BATCH",
      "code": "RAW-SUGAR-001",
      "name": "Duong",
      "actorName": "Nha cung cap Y",
      "hasInputs": false,
      "verificationStatus": "VERIFIED"
    }
  ],
  "verificationScope": "CURRENT_AND_DIRECT_INPUTS",
  "verificationSummary": {
    "currentNodeStatus": "VERIFIED",
    "inputRelationStatus": "VERIFIED",
    "verifiedInputCount": 2,
    "totalInputCount": 2,
    "overallStatus": "VERIFIED"
  }
}
```

---

## 10. API verify tung cap

### Endpoint

```http
POST /product/api/v1/pallets/{palletId}/verify-direct
```

Voi unit, mobile lay `palletId` tu response trace ban dau va goi endpoint nay.

### Pham vi verify

Moi request chi verify:

```text
1. dataHash cua pallet dang xem.
2. parentRoot cua pallet dang xem.
3. dataHash cua cac input truc tiep.
```

Khong verify input cua cac input pallet trong request hien tai.

Neu khach hang bam vao pallet sua dac, request moi se verify:

```text
pallet sua dac + parentRoot cua no + input truc tiep cua no
```

### Trang thai

```text
NOT_VERIFIED
VERIFIED
MISMATCH
NOT_ANCHORED
VERIFY_FAILED
```

### Cau thong bao tren mobile

Dung:

```text
Da xac minh lo dang xem va cac nguyen lieu dau vao truc tiep.
```

Khong dung:

```text
Toan bo chuoi nguon goc da duoc xac minh.
```

---

## 11. Blockchain

### Smart contract

Khong can thay doi contract.

Contract hien tai da co:

```solidity
recordBatch(batchId, dataHash)
recordTransformedBatch(batchId, dataHash, parentHashes)
getBatchRecord(batchId)
getTransformedBatchRecord(batchId)
```

Pallet input co the la RawBatch hoac Pallet, vi ca hai deu co blockchain batch
ID dang bytes32.

### Cach ghi pallet moi

Tao danh sach parent blockchain IDs:

```text
RawBatch input -> rawBatch.batchIdHex
Pallet input   -> pallet.chainBatchIdHex
```

Sort danh sach va gui vao:

```solidity
recordTransformedBatch(...)
```

Contract tinh:

```solidity
parentRoot = keccak256(abi.encodePacked(parentHashes))
```

### Phan thieu hien tai can sua

API `verifyHashes` hien tai moi so sanh `dataHash`. No chua xac minh
`parentRoot`.

Can bo sung mot trong hai cach:

#### Cach de xuat

Them API blockchain service:

```http
POST /api/v1/blockchain/verify-transformed-direct
```

Request:

```json
{
  "batchIdHex": "output-pallet-chain-id",
  "dataHashHex": "recalculated-output-data-hash",
  "parentBatchIdHexes": [
    "input-chain-id-1",
    "input-chain-id-2"
  ]
}
```

Blockchain service:

1. Doc `dataHash` va `parentRoot` tu contract.
2. Sort parent IDs bang quy tac giong luc ghi.
3. Tinh lai `expectedParentRoot`.
4. So sanh ca `dataHash` va `parentRoot`.
5. Tra hai ket qua rieng.

Response:

```json
{
  "dataHashMatch": true,
  "parentRootMatch": true
}
```

Sau do Core dung `verifyHashes` hien tai de verify dataHash cua cac input truc
tiep theo batch.

### Dieu kien bat buoc

Quy tac sort va normalize parent IDs luc ghi va luc verify phai giong nhau
tuyet doi.

---

## 12. Sua logic trace hien tai

`ProductUnitServiceImpl.buildHistoryEvents(...)` hien dang hard-code:

```text
Pallet -> parentRawBatchIdHexes -> RawBatch
```

Can tach thanh cac service:

```text
DirectTraceService
DirectVerificationService
PalletInputResolver
```

Trach nhiem:

```text
DirectTraceService:
    lay current pallet va direct inputs

DirectVerificationService:
    verify current pallet, parentRoot va direct inputs

PalletInputResolver:
    resolve RAW_BATCH/PALLET thanh DTO chung
```

Trong giai doan chuyen doi:

```text
- Uu tien doc tu pallet_inputs.
- Neu pallet cu chua co pallet_inputs, fallback parentRawBatchIdHexes.
```

Sau khi backfill du lieu duoc doi chieu, bo fallback.

---

## 13. Frontend web

### 13.1. Don hang M2M

Them man/dat don:

```text
Nha san xuat mua lo tu nha san xuat
```

Cho phep chon:

```text
- Seller manufacturer
- Pallet dang AVAILABLE
- Thong tin san pham
- Ma lo/pallet
- Ngay san xuat
- Trang thai blockchain
```

### 13.2. Man tao pallet

Thay truong:

```text
Raw batch nguon
```

bang:

```text
Input san xuat
```

Chia input dang so huu thanh:

```text
- Raw batches
- Pallets mua tu nha san xuat khac
- Pallets noi bo neu duoc phep
```

Moi input hien thi:

```text
Loai input
Ten
Ma lo
Chu so huu
Nha san xuat ban dau
Trang thai AVAILABLE/CONSUMED
```

### 13.3. Man chi tiet pallet

Them:

```text
- Input truc tiep da su dung
- Output pallet da tieu thu pallet nay, neu co
- Trang thai input_status
- Trang thai verify
```

---

## 14. Frontend mobile

### 14.1. Man trace ban dau

Sau khi quet unit, hien thi:

```text
Thong tin san pham
Thong tin lo thanh pham
Danh sach nguyen lieu dau vao truc tiep
Nut xac minh cap hien tai
```

### 14.2. Moi input card

Hien thi:

```text
Ten input
Loai RAW_BATCH/PALLET
Nha cung cap/nha san xuat
Dia diem
Trang thai verify
```

Neu `hasInputs = true`, hien thi action:

```text
Xem nguon goc sau hon
```

Neu `hasInputs = false`, hien thi:

```text
Nguyen lieu goc
```

### 14.3. Dieu huong sau hon

Khi bam pallet input:

```text
GET /product/api/v1/pallets/{id}/trace-direct
```

Mo trang trace cung cau truc cho pallet do.

Khong can tai toan bo graph vao memory. Navigation stack cua mobile dai dien
cho duong truy xuat nguoi dung da mo.

### 14.4. Verify

Nut:

```text
Xac minh cap hien tai
```

Goi:

```text
POST /product/api/v1/pallets/{id}/verify-direct
```

Hien thi:

```text
Da xac minh lo dang xem va N dau vao truc tiep.
```

Neu co loi:

```text
Phat hien sai lech tai lo sua dac PALLET-MILK-001.
```

---

## 15. Quyen truy cap va du lieu cong khai

Trace va verify cho khach hang co the public, nhung khong nen tra:

```text
- ownerId noi bo
- thong tin thuong mai nhay cam
- gia mua
- ghi chu noi bo
- chi tiet loi he thong
```

Thong tin cong khai de xuat:

```text
- Ma lo
- Ten san pham/nguyen lieu
- Nha san xuat/nha cung cap
- Vi tri
- Ngay san xuat/thu hoach
- Trang thai blockchain
- Quan he input truc tiep
```

Neu doanh nghiep can an mot input nhay cam, he thong van phai cho biet co mot
input bi gioi han quyen xem, khong duoc am tham bo no khoi so input.

---

## 16. Hieu nang

Vi moi request chi tai mot cap, response duoc gioi han tu nhien.

Van can:

```text
- Index pallet_inputs(output_pallet_id).
- Index pallet_inputs(input_type, input_id).
- Batch query direct inputs.
- Batch verify input hashes trong mot request verifyHashes.
- Cache actor/catalog data.
- Khong goi blockchain trong trace thong thuong.
```

Them gioi han:

```text
max direct inputs per pallet: 100
request timeout cho verify: 10 giay
```

Neu mot pallet co qua nhieu input, API can pagination hoac chi tra summary
truoc.

---

## 17. Kiem thu

### Unit test

```text
- Tao pallet chi tu RawBatch.
- Tao pallet tu mot Pallet khac.
- Tao pallet tu RawBatch va Pallet.
- Chan input khong thuoc owner.
- Chan pallet input da CONSUMED.
- Chan pallet tu tro vao chinh no.
- Chan cycle.
- Parent IDs duoc sort on dinh.
- parentRoot verify dung khi input khong doi.
- parentRoot mismatch khi sua quan he input.
```

### Integration test chinh

```text
1. Supplier tao RawBatch sua bo.
2. Nha may A mua RawBatch.
3. Nha may A tao Pallet sua dac tu RawBatch sua bo.
4. Nha may B dat mua Pallet sua dac tu Nha may A.
5. Giao hang va chuyen owner Pallet sua dac sang B.
6. Nha may B tao Pallet banh sua tu Pallet sua dac.
7. Nha may B dong goi Carton va ProductUnit.
8. Khach hang quet Unit banh sua.
9. Khach hang thay Pallet sua dac la input truc tiep.
10. Khach hang verify cap banh sua.
11. Khach hang bam Pallet sua dac.
12. Khach hang thay RawBatch sua bo va verify cap sua dac.
```

### Test tinh toan ven

```text
- Sua du lieu pallet sau khi anchor -> dataHash MISMATCH.
- Sua pallet_inputs sau khi anchor -> parentRoot MISMATCH.
- Sua raw batch input -> input status MISMATCH.
```

### Test tuong thich

```text
- Unit cu van trace duoc sau migration.
- Pallet cu co direct inputs sau backfill.
- Don MANUFACTURER_TO_SUPPLIER cu van hoat dong.
- Don RETAILER_TO_MANUFACTURER cu van hoat dong.
```

---

## 18. Thu tu trien khai

### Phase 1: Database va domain

```text
1. Tao pallet_inputs.
2. Them pallet.input_status.
3. Backfill quan he RawBatch cu.
4. Them entity/repository.
```

### Phase 2: San xuat

```text
5. Sua PalletAnchorRequest.
6. Sua PalletServiceImpl.
7. Tao PalletInput khi san xuat.
8. Danh dau Pallet input CONSUMED.
```

### Phase 3: Trade M2M

```text
9. Them MANUFACTURER_TO_MANUFACTURER.
10. Them targetPalletId vao order line.
11. Them validation va lifecycle RESERVED/AVAILABLE.
12. Chuyen owner pallet/carton/unit khi giao thanh cong.
```

### Phase 4: Trace va verify

```text
13. Tao DirectTraceService.
14. Them API trace-direct.
15. Them verify parentRoot tai blockchain service.
16. Tao DirectVerificationService.
17. Them API verify-direct.
18. Bo sung directInputs vao trace unit.
```

### Phase 5: Giao dien

```text
19. Them man don M2M tren web.
20. Sua man tao pallet tren web.
21. Sua mobile trace de hien input truc tiep.
22. Them dieu huong xem sau hon.
23. Them nut verify cap hien tai.
```

### Phase 6: Hoan thien

```text
24. Chay integration test end-to-end.
25. Kiem tra hieu nang.
26. Doi chieu du lieu backfill.
27. Cap nhat tai lieu bao cao va kich ban demo.
```

---

## 19. Kich ban demo do an

Kich ban demo de xuat:

```text
Trang trai X tao lo sua bo.
Nha may A mua lo sua bo va tao pallet sua dac.
Nha may B mua pallet sua dac tu Nha may A.
Nha may B dung pallet sua dac de tao pallet banh sua.
Nha may B dong goi banh sua thanh unit co QR.
Khach hang quet QR va thay sua dac la input truc tiep.
Khach hang verify pallet banh sua va input truc tiep.
Khach hang bam vao sua dac de thay sua bo tu Trang trai X.
Khach hang verify cap sua dac.
```

Kich ban nay the hien:

```text
- Chuoi cung ung nhieu nha may.
- Dat hang va chuyen quyen so huu.
- Quan he input-output nhieu tang.
- Truy xuat tung cap.
- Blockchain dataHash verification.
- Blockchain parentRoot verification.
- Mobile lazy loading.
```

---

## 20. Definition of Done

Tinh nang hoan thanh khi:

```text
- NSX co the dat mua pallet tu NSX khac.
- Pallet mua ve co the duoc dung lam input san xuat.
- Moi pallet luu duoc input truc tiep bang pallet_inputs.
- Khong the dung lai pallet da CONSUMED.
- Khong the tao cycle.
- Unit cu va moi van truy xuat duoc.
- Khach hang xem duoc input truc tiep.
- Khach hang bam input pallet de xem sau hon.
- Moi lan verify chi verify cap dang xem va input truc tiep.
- dataHash va parentRoot deu duoc doi chieu blockchain.
- Mobile thong bao ro pham vi da verify.
- Smart contract hien tai khong can deploy lai.
```

---

## 21. Huong phat trien sau do an

Neu can mo rong quy mo production, co the:

```text
- Tach lo san xuat khoi pallet vat ly thanh TraceBatch.
- Ho tro dung mot phan lo dau vao.
- Quan ly remaining quantity va quy doi don vi.
- Ho tro split/merge batch.
- Ho tro verify toan chuoi bat dong bo cho auditor.
- Ho tro thu hoi va tim tat ca output bi anh huong.
- Tich hop GS1 EPCIS TransformationEvent.
```

# Ke hoach trien khai Claim QR va danh gia san pham

## 1. Muc tieu va quyet dinh pham vi

Tai lieu nay mo ta ke hoach trien khai chuc nang danh gia san pham danh cho
nguoi dung cuoi co role `USER`.

He thong khong co gang chung minh nguoi dung da thanh toan tai sieu thi.
Thay vao do, he thong chung minh nguoi dung co quyen truy cap ma Claim QR bi
mat nam tren san pham thuc te.

Moi `ProductUnit` se co hai ma QR:

```text
QR truy xuat:
    chua unitId hoac unitSerial hien tai
    duoc dan ben ngoai bao bi
    ai cung co the quet

QR claim:
    chi chua mot chuoi token ngau nhien, vi du CLM-...
    duoc che boi lop cao hoac nam trong bao bi
    chi duoc dung de tao mot danh gia da xac minh
```

Danh gia tao bang Claim QR se hien thi nhan:

```text
Da xac minh so huu/su dung san pham
```

Khong hien thi nhan `Da xac minh mua hang`, vi Claim QR khong phai bang chung
thanh toan.

Pham vi do an:

- Role `USER` co them chuc nang `Danh gia`.
- Nguoi dung quet Claim QR trong ung dung.
- Quet thanh cong thi hien thong tin san pham va form danh gia.
- Danh gia gom 1-5 sao, noi dung, toi da 5 anh va 1 video ngan.
- Mot Claim QR chi tao duoc mot danh gia.
- Danh gia co the duoc chinh sua boi chinh nguoi tao.
- Claim QR khong ghi len blockchain.
- Anh va video luu tren Cloudinary; metadata luu trong PostgreSQL.

---

## 2. Nguyen tac nghiep vu

### 2.1. Claim QR chua gi

Claim QR chi chua mot chuoi:

```text
CLM-<random-token>
```

Vi du:

```text
CLM-7TQ5V8M2RK3X9N6D4PAWZJHC
```

Token:

- Duoc sinh bang bo sinh so ngau nhien bao mat.
- Co toi thieu 128 bit entropy.
- Khong duoc suy ra tu `unitSerial`, `unitId` hoac `productId`.
- Khong chua JSON, URL, userId hay thong tin san pham.
- Co prefix `CLM-` de mobile phan biet voi QR truy xuat.
- Khong ghi token goc vao log.

### 2.2. Khi nao claim duoc su dung

Quet Claim QR chi de kiem tra va hien form danh gia. Khong danh dau claim da
su dung ngay khi quet.

Claim chi chuyen tu `AVAILABLE` sang `CLAIMED` trong cung transaction tao
danh gia:

```text
Khoa claim trong DB
    -> kiem tra claim AVAILABLE
    -> kiem tra media hop le
    -> tao review
    -> gan claim cho reviewer
    -> chuyen claim thanh CLAIMED
    -> commit
```

Neu nguoi dung quet roi thoat, Claim QR van co the su dung lai.

### 2.3. Quet lai Claim QR

```text
Claim AVAILABLE:
    hien thong tin san pham va form tao danh gia

Claim CLAIMED boi chinh user dang dang nhap:
    hien danh gia da tao va cho phep chinh sua

Claim CLAIMED boi user khac:
    thong bao ma da duoc su dung

Claim REVOKED:
    thong bao ma khong con hieu luc

Token khong ton tai:
    thong bao Claim QR khong hop le
```

### 2.4. Gioi han danh gia

- Mot `product_unit_claim` chi gan voi mot `product_review`.
- Mot `ProductUnit` chi co mot claim dang hoat dong.
- Rating tu 1 den 5.
- Noi dung co the rong neu nguoi dung da chon sao.
- Toi da 5 anh.
- Toi da 1 video.
- Anh toi da 5 MB/file.
- Video toi da 50 MB va 30 giay.
- Review mac dinh `PUBLISHED` trong pham vi do an.
- Admin co the chuyen review sang `HIDDEN`.

---

## 3. Vi tri trien khai trong kien truc hien tai

Chuc nang dat trong `traceability-core-service`.

Ly do:

- Service nay dang so huu `ProductUnit`.
- Claim gan 1-1 voi `ProductUnit`.
- Service da co PostgreSQL migration, JWT security va Cloudinary config.
- Viec claim va tao review can transaction tren cung database.

Khong dat claim/review trong `catalog-service`, vi catalog chi so huu thong
tin chung cua `Product`, khong so huu tung unit.

Khong can blockchain-service, Kafka hay WebSocket cho luong nay.

```text
Mobile USER
    -> API Gateway
        -> traceability-core-service
            -> PostgreSQL
            -> Cloudinary
            -> catalog-service (lay thong tin Product khi can)
```

---

## 4. Mo hinh du lieu

### 4.1. Bang `product_unit_claims`

```text
id UUID PK
product_unit_id UUID NOT NULL FK -> product_units.id
claim_token_hash VARCHAR(64) NOT NULL
status VARCHAR(20) NOT NULL
claimed_by_user_id VARCHAR(36) NULL
claimed_at TIMESTAMP NULL
created_at TIMESTAMP NOT NULL
revoked_at TIMESTAMP NULL
```

Trang thai:

```text
AVAILABLE
CLAIMED
REVOKED
```

Rang buoc:

```sql
UNIQUE (product_unit_id)
UNIQUE (claim_token_hash)
CHECK (status IN ('AVAILABLE', 'CLAIMED', 'REVOKED'))
```

Index:

```sql
CREATE UNIQUE INDEX ... ON product_unit_claims(claim_token_hash);
CREATE INDEX ... ON product_unit_claims(claimed_by_user_id, claimed_at DESC);
```

`claim_token_hash` la HMAC-SHA256 cua token:

```text
HMAC-SHA256(CLAIM_HMAC_SECRET, claimToken)
```

Khong luu claim token goc trong DB.

### 4.2. Bang `product_reviews`

```text
id UUID PK
claim_id UUID NOT NULL FK -> product_unit_claims.id
product_unit_id UUID NOT NULL FK -> product_units.id
product_id VARCHAR(36) NOT NULL
reviewer_id VARCHAR(36) NOT NULL
rating SMALLINT NOT NULL
content TEXT NULL
status VARCHAR(20) NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
```

Trang thai:

```text
PUBLISHED
HIDDEN
```

Rang buoc:

```sql
UNIQUE (claim_id)
UNIQUE (product_unit_id)
CHECK (rating BETWEEN 1 AND 5)
CHECK (status IN ('PUBLISHED', 'HIDDEN'))
```

Index:

```sql
CREATE INDEX ... ON product_reviews(product_id, status, created_at DESC);
CREATE INDEX ... ON product_reviews(reviewer_id, created_at DESC);
```

### 4.3. Bang `review_media`

```text
id UUID PK
review_id UUID NULL FK -> product_reviews.id
uploader_id VARCHAR(36) NOT NULL
media_type VARCHAR(10) NOT NULL
media_url TEXT NOT NULL
thumbnail_url TEXT NULL
cloudinary_public_id VARCHAR(255) NOT NULL
file_size BIGINT NOT NULL
duration_seconds INTEGER NULL
sort_order INTEGER NOT NULL
status VARCHAR(20) NOT NULL
created_at TIMESTAMP NOT NULL
attached_at TIMESTAMP NULL
```

Loai media:

```text
IMAGE
VIDEO
```

Trang thai:

```text
UPLOADED
ATTACHED
DELETED
```

Rang buoc:

```sql
CHECK (media_type IN ('IMAGE', 'VIDEO'))
CHECK (status IN ('UPLOADED', 'ATTACHED', 'DELETED'))
```

`review_id` de null trong luc media da upload nhung review chua duoc tao.

Can co job don media `UPLOADED` khong duoc gan vao review sau 24 gio.

### 4.4. Migration

Tao migration Flyway moi trong:

```text
backend-services/traceability-core-service/src/main/resources/db/migration/
```

Migration phai:

1. Tao ba bang moi.
2. Tao FK, unique constraint, check constraint va index.
3. Khong them lai cac cot secret/claim truc tiep vao `product_units`.
4. Khong sua du lieu unit hien co.

Claim cho unit cu duoc sinh bang mot API backfill rieng neu can demo.

---

## 5. Sinh Claim QR cung ProductUnit

### 5.1. Thay doi luong generate unit

Trong `ProductUnitServiceImpl.generateUnits`:

```text
Tao ProductUnit
    -> sinh claimToken ngau nhien
    -> tinh claimTokenHash
    -> tao ProductUnitClaim AVAILABLE
    -> tra ve cap QR trong response
```

Moi item trong `ProductUnitGenerateResponse` bo sung:

```text
unitId
unitSerial
traceQrPayload
claimToken
```

Vi du:

```json
{
  "unitId": "uuid",
  "unitSerial": "CTN-001-U0001",
  "traceQrPayload": "uuid",
  "claimToken": "CLM-7TQ5V8M2RK3X9N6D4PAWZJHC"
}
```

`claimToken` chi duoc tra ve trong response sinh unit. Cac API doc unit,
trace unit va danh sach unit khong duoc tra token nay.

### 5.2. In cap tem

Giao dien nha san xuat can co chuc nang `Tai/In cap tem QR`.

Moi dong tem phai hien:

```text
Tem truy xuat:
    QR traceQrPayload
    unitSerial

Tem claim:
    QR claimToken
    nhan "QR danh gia - cao de quet"
    ma don vi rut gon de doi chieu neu can
```

Hai QR cua cung unit phai nam cung mot dong/nhom trong file in de tranh dan
nham.

Phuong an trien khai do an:

1. API generate tra du lieu cap QR.
2. Frontend web tao file in/PDF ngay sau khi generate.
3. Nha san xuat tai va in file ngay.

Khong luu token goc nen khong the tai lai dung Claim QR cu.

Neu mat file hoac can in lai:

```text
POST /api/v1/units/{unitId}/claim/regenerate
```

API chi cho owner/manufacturer thuc hien khi claim chua `CLAIMED`.
Token cu bi vo hieu hoa va token moi duoc tra ve mot lan.

### 5.3. Bien moi truong

Them:

```text
CLAIM_HMAC_SECRET=<secret rieng, toi thieu 32 bytes>
```

Khong dung chung voi `JWT_SIGNER_KEY`.

Khong commit secret that vao repository.

---

## 6. API backend

Tat ca API claim va review ghi du lieu deu yeu cau JWT.

### 6.1. Resolve Claim QR

```http
POST /api/v1/claims/resolve
Authorization: Bearer <USER token>
```

Request:

```json
{
  "claimToken": "CLM-..."
}
```

Response khi AVAILABLE:

```json
{
  "claimStatus": "AVAILABLE",
  "productUnitId": "...",
  "unitSerial": "...",
  "productId": "...",
  "productName": "...",
  "productImageUrl": "...",
  "manufacturerName": "...",
  "existingReview": null
}
```

Response khi CLAIMED boi chinh user:

```json
{
  "claimStatus": "CLAIMED_BY_ME",
  "existingReview": { "...": "..." }
}
```

API nay:

- Khong doi trang thai claim.
- Khong tra `claimTokenHash`.
- Chi chap nhan role `USER`.
- Co rate limit theo userId va IP.

### 6.2. Upload media tam

```http
POST /api/v1/review-media
Authorization: Bearer <USER token>
Content-Type: multipart/form-data
```

Form:

```text
file=<binary>
mediaType=IMAGE|VIDEO
```

Response:

```json
{
  "mediaId": "...",
  "mediaType": "IMAGE",
  "mediaUrl": "...",
  "thumbnailUrl": "...",
  "status": "UPLOADED"
}
```

Backend phai:

- Kiem tra MIME thuc te, khong chi extension.
- Chan file khong dung loai.
- Kiem tra uploader tu JWT.
- Upload anh voi Cloudinary `resource_type=image`.
- Upload video voi Cloudinary `resource_type=video`.
- Lay `public_id`, URL, thumbnail, kich thuoc va duration tu Cloudinary.
- Khong cho user gan media cua user khac vao review.

Can mo rong `CloudinaryService`, vi hien tai chi ho tro `uploadImage`.

Can tang multipart limit cua core service:

```yaml
spring.servlet.multipart.max-file-size: 50MB
spring.servlet.multipart.max-request-size: 55MB
```

Mobile van phai nen file truoc khi upload.

### 6.3. Tao review va consume claim

```http
POST /api/v1/reviews
Authorization: Bearer <USER token>
```

Request:

```json
{
  "claimToken": "CLM-...",
  "rating": 5,
  "content": "San pham ngon",
  "mediaIds": ["...", "..."]
}
```

Backend xu ly trong mot transaction:

1. Validate token format.
2. Tinh HMAC va tim claim bang row lock `FOR UPDATE`.
3. Kiem tra claim `AVAILABLE`.
4. Validate rating/noi dung.
5. Tai cac media theo `mediaIds`.
6. Kiem tra media thuoc user, dang `UPLOADED`, dung gioi han 5 anh/1 video.
7. Tao `ProductReview`.
8. Gan media vao review va chuyen `ATTACHED`.
9. Chuyen claim sang `CLAIMED`.
10. Gan `claimedByUserId`, `claimedAt`.
11. Commit.

Neu bat ky buoc nao loi, rollback toan bo va claim van `AVAILABLE`.

### 6.4. Sua va doc review

```http
PUT /api/v1/reviews/{reviewId}
GET /api/v1/reviews/my
GET /api/v1/products/{productId}/reviews?page=0&size=20
GET /api/v1/products/{productId}/review-summary
PATCH /api/v1/admin/reviews/{reviewId}/status
```

Quyen:

```text
USER:
    resolve claim
    upload media
    tao review
    sua review cua minh
    xem review cua minh

Public:
    xem review PUBLISHED va tong hop rating

ADMIN:
    an/hien review

MANUFACTURER:
    chi doc review cua product minh so huu
    khong duoc sua/xoa review
```

Review response nen co:

```text
reviewId
rating
content
reviewerDisplayName
reviewerAvatarUrl
verifiedOwnership = true
media[]
createdAt
updatedAt
```

Khong tra `reviewerId` tren API public neu khong can.

---

## 7. Mobile role USER

### 7.1. Navigation

Trong `MainPage`, chi role `USER` co them tab:

```text
Danh gia
```

Navigation du kien:

```text
Quet QR | Danh gia | Lich su | Ca nhan
```

Role khac giu giao dien hien tai.

### 7.2. Man quet Claim QR

Tai tab `Danh gia`:

1. Hien camera quet QR.
2. Chi chap nhan chuoi bat dau bang `CLM-`.
3. Goi `/claims/resolve`.
4. Hien loading trong luc resolve.
5. Neu hop le, chuyen den man thong tin san pham va form danh gia.
6. Neu claim da dung boi nguoi khac, hien thong bao ro rang.

Khong dung chung man quet truy xuat de tranh nham muc dich.

### 7.3. Form danh gia

Man form gom:

- Anh va ten san pham.
- Unit serial rut gon.
- Nhan `Da xac minh so huu san pham`.
- Bo chon 1-5 sao.
- O nhap noi dung.
- Nut them anh.
- Nut them video.
- Preview media co the xoa/sap xep.
- Thanh tien trinh upload.
- Nut gui danh gia.

Nut gui chi bat khi:

- Rating hop le.
- Khong co media dang upload.
- Tat ca media upload thanh cong.

Neu tao review that bai, giu form va media de user thu lai.

### 7.4. Anh va video tren mobile

Su dung `image_picker` hien co de:

- Chon/chup anh.
- Chon/quay video.

Bo sung thu vien nen media neu can, uu tien:

```text
flutter_image_compress
video_compress
video_player
```

Can kiem tra tuong thich Android/iOS truoc khi chot dependency.

UI phai:

- Bao dung luong va thoi luong video truoc upload.
- Khong cho chon qua 5 anh/1 video.
- Hien thumbnail video va nut play.
- Hien trang thai upload rieng cho tung file.

### 7.5. Hien thi danh gia

Them khu vuc danh gia vao ket qua truy xuat san pham:

- Diem trung binh va tong so danh gia.
- Phan bo 1-5 sao.
- Danh sach review PUBLISHED.
- Anh dang luoi, bam de xem fullscreen.
- Video hien thumbnail va chi phat khi user bam.
- Nhan `Da xac minh so huu san pham`.

Khong tu dong tai/phat video khi mo man truy xuat.

---

## 8. Frontend web nha san xuat va admin

### 8.1. Nha san xuat

Sau khi generate unit:

- Hien so unit da tao.
- Cho tai/in file cap QR.
- Canh bao Claim QR chi xuat hien mot lan.
- Neu dong popup ma chua tai, hien xac nhan.
- Ho tro regenerate Claim QR cho unit chua claim.
- Hien tong hop review tren tung product.

### 8.2. Admin

Them man quan ly review:

- Loc theo `PUBLISHED`, `HIDDEN`.
- Xem anh/video.
- An review vi pham.
- Khong duoc sua noi dung review cua user.

---

## 9. Bao mat va chong lam dung

### Claim token

- Token ngau nhien bao mat, toi thieu 128 bit.
- Luu HMAC-SHA256, khong luu token goc.
- So sanh bang hash.
- Khong log request body cua endpoint claim/review.
- Rate limit `/claims/resolve` va `/reviews`.
- Khoa row claim khi tao review de chong hai request dong thoi.
- Token da claim khong duoc gan cho user khac.

### Media

- Validate MIME thuc te va kich thuoc.
- Doi ten/public ID do server tao.
- Khong tin `mediaType` do client gui.
- Khong cho gan media cua user khac.
- Xoa media mo coi.
- Khong cho URL tuy y tu client.
- Cloudinary credential phai lay tu environment, khong commit secret.

### Review

- Reviewer lay tu JWT, khong lay tu request body.
- Product/unit lay tu claim, khong lay tu request body.
- Public chi doc review `PUBLISHED`.
- Escape/sanitize noi dung khi hien tren web.
- Gioi han chieu dai noi dung, vi du 2.000 ky tu.

---

## 10. Xu ly loi va tinh huong bien

Can co error code ro rang:

```text
INVALID_CLAIM_TOKEN
CLAIM_NOT_FOUND
CLAIM_ALREADY_USED
CLAIM_REVOKED
CLAIM_NOT_OWNED_BY_USER
REVIEW_ALREADY_EXISTS
INVALID_RATING
MEDIA_LIMIT_EXCEEDED
MEDIA_NOT_OWNED
MEDIA_INVALID_TYPE
MEDIA_UPLOAD_FAILED
VIDEO_TOO_LONG
FILE_TOO_LARGE
```

Tinh huong can test:

- Hai user gui review cung mot Claim QR cung luc.
- User quet claim roi thoat.
- User upload media roi khong tao review.
- Upload mot phan media thanh cong, mot phan that bai.
- Claim token bi regenerate sau khi tem bi mat.
- User quet lai claim cua chinh minh.
- Admin an review.
- Product/unit bi xoa hoac khong ton tai.
- Cloudinary tam thoi khong hoat dong.

---

## 11. Ke hoach trien khai theo giai doan

### Giai doan 1: Claim backend va cap QR

1. Tao migration `product_unit_claims`.
2. Tao entity, repository, enum status.
3. Tao `ClaimTokenService` sinh token va HMAC.
4. Them `CLAIM_HMAC_SECRET`.
5. Sua generate unit de tao claim.
6. Mo rong response generate unit.
7. Tao API regenerate claim.
8. Tao API resolve claim.
9. Them unit/integration test tranh claim trung va do token.

Ket qua:

```text
Moi unit moi co cap QR trace + claim.
Role USER quet claim va xem duoc thong tin san pham.
```

### Giai doan 2: Review khong media

1. Tao migration `product_reviews`.
2. Tao entity/repository/service/controller.
3. Tao review va consume claim trong cung transaction.
4. Them API sua, xem review cua toi va danh sach public.
5. Them summary rating.
6. Them mobile tab `Danh gia`, scanner va form sao/noi dung.

Ket qua:

```text
USER quet Claim QR va tao mot review da xac minh.
```

### Giai doan 3: Anh va video

1. Tao migration `review_media`.
2. Mo rong CloudinaryService cho image/video/delete.
3. Tao API upload media tam.
4. Validate gioi han media khi tao/sua review.
5. Tao job don media mo coi.
6. Them picker, compression, upload progress va preview tren mobile.
7. Them hien thi gallery/video trong danh sach review.

Ket qua:

```text
Review ho tro toi da 5 anh va 1 video ngan.
```

### Giai doan 4: In tem va quan tri

1. Them giao dien tai/in cap tem QR tren frontend web.
2. Them regenerate claim cho unit chua claim.
3. Them man tong hop review cho manufacturer.
4. Them man an/hien review cho admin.
5. Them rate limit va audit log.

### Giai doan 5: Hoan thien va kiem thu

1. Test backend transaction va concurrency.
2. Test API security theo role.
3. Test migration tren DB co du lieu cu.
4. Test Flutter Android/iOS: camera, gallery, video, permission.
5. Test media dung luong lon va mang cham.
6. Test cap QR khong bi dan nham.
7. Test end-to-end:

```text
Generate unit
    -> in cap QR
    -> quet trace QR
    -> cao va quet Claim QR
    -> xem san pham
    -> upload anh/video
    -> tao review
    -> xem review tren trang san pham
    -> quet lai claim
```

---

## 12. Thu tu file/module du kien can sua

### Backend `traceability-core-service`

```text
db/migration/Vxx__create_product_unit_claims.sql
db/migration/Vxx__create_product_reviews.sql
db/migration/Vxx__create_review_media.sql

entity/ProductUnitClaim.java
entity/ProductReview.java
entity/ReviewMedia.java

repository/ProductUnitClaimRepository.java
repository/ProductReviewRepository.java
repository/ReviewMediaRepository.java

service/ClaimTokenService.java
service/ProductClaimService.java
service/ProductReviewService.java
service/ReviewMediaService.java
service/CloudinaryService.java

controller/ProductClaimController.java
controller/ProductReviewController.java
controller/ReviewMediaController.java

dto/request/*
dto/response/*

service/impl/ProductUnitServiceImpl.java
dto/response/ProductUnitGeneratedItem.java
application.yml
```

### Gateway/security

```text
api-gateway/PublicRouteMatcher.java
traceability-core-service/ProductPublicPathMatcher.java
```

Chi endpoint doc review public. Resolve claim, upload media va ghi review bat
buoc co JWT.

### Frontend mobile

```text
features/review/
    data/
    domain/
    presentation/

features/main/presentation/pages/main_page.dart
features/main/presentation/pages/trace_result_page.dart
pubspec.yaml
```

### Frontend web

```text
manufacture/components/.../UnitLabelPrint.*
manufacture/components/.../ProductReviewSummary.*
admin/components/.../ReviewModeration.*
```

---

## 13. Tieu chi chap nhan

Chuc nang duoc xem la hoan thanh khi:

1. Moi unit moi co mot Claim QR token ngau nhien rieng.
2. DB chi luu HMAC, khong luu token goc.
3. Cap QR truy xuat/claim duoc tao cung nhau va co the in.
4. Role `USER` co tab `Danh gia` va quet duoc chuoi `CLM-...`.
5. Quet claim hop le hien dung thong tin san pham.
6. Claim khong bi consume khi chi quet.
7. Tao review thanh cong moi consume claim.
8. Hai user khong the cung tao review bang mot claim.
9. Review ho tro 1-5 sao, noi dung, 5 anh va 1 video.
10. Media upload loi khong lam mat quyen claim.
11. Review public hien nhan `Da xac minh so huu san pham`.
12. User quet lai claim cua minh xem/sua duoc review cu.
13. Admin an duoc review vi pham.
14. Test migration, backend, Flutter va end-to-end deu dat.

---

## 14. Ngoai pham vi hien tai

- Xac minh thanh toan tai POS/sieu thi.
- Hoan tien hoac tranh chap quyen so huu.
- Cho phep nhieu review tren mot unit.
- Ghi review, claim hoac media hash len blockchain.
- AI kiem duyet noi dung/anh/video.
- Livestream video hoac transcoding nang.
- Chia se Claim QR giua nhieu tai khoan.

Day la cac huong co the phat trien sau khi hoan thanh do an.

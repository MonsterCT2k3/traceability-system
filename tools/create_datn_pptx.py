from __future__ import annotations

import os
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape


ROOT = Path(r"D:\Documents\Workspace\traceability-system")
ASSETS = ROOT / "slide_assets"
OUT = ROOT / "DATN_Nguyen_Dang_Nam_Traceability_Blockchain.pptx"

EMU = 914400
SLIDE_W = int(13.333333 * EMU)
SLIDE_H = int(7.5 * EMU)

COLORS = {
    "bg": "F8FAFC",
    "ink": "0F172A",
    "muted": "475569",
    "line": "CBD5E1",
    "blue": "2563EB",
    "blue2": "DBEAFE",
    "green": "16A34A",
    "green2": "DCFCE7",
    "orange": "F97316",
    "orange2": "FFEDD5",
    "red": "DC2626",
    "red2": "FEE2E2",
    "white": "FFFFFF",
}


def emu(v: float) -> int:
    return int(v * EMU)


def xesc(s: str) -> str:
    return escape(str(s), {'"': "&quot;"})


def solid_fill(color: str) -> str:
    return f'<a:solidFill><a:srgbClr val="{color}"/></a:solidFill>'


def line_xml(color: str = "CBD5E1", width: int = 9525) -> str:
    return f'<a:ln w="{width}"><a:solidFill><a:srgbClr val="{color}"/></a:solidFill></a:ln>'


def shape(
    sid: int,
    x: float,
    y: float,
    w: float,
    h: float,
    fill: str,
    line: str = "CBD5E1",
    radius: str = "roundRect",
) -> str:
    return f"""
<p:sp>
  <p:nvSpPr><p:cNvPr id="{sid}" name="Shape {sid}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>
    <a:prstGeom prst="{radius}"><a:avLst/></a:prstGeom>
    {solid_fill(fill)}
    {line_xml(line)}
  </p:spPr>
  <p:txBody><a:bodyPr/><a:lstStyle/><a:p/></p:txBody>
</p:sp>"""


def text_box(
    sid: int,
    x: float,
    y: float,
    w: float,
    h: float,
    text: str,
    size: int = 24,
    color: str = "0F172A",
    bold: bool = False,
    align: str = "l",
    valign: str = "top",
) -> str:
    lines = str(text).split("\n")
    paras = []
    for line in lines:
        b = ' b="1"' if bold else ""
        paras.append(
            f'<a:p><a:pPr algn="{align}"/>'
            f'<a:r><a:rPr lang="vi-VN" sz="{size * 100}"{b}>'
            f'<a:solidFill><a:srgbClr val="{color}"/></a:solidFill>'
            f'<a:latin typeface="Aptos"/></a:rPr><a:t>{xesc(line)}</a:t></a:r>'
            f'<a:endParaRPr lang="vi-VN" sz="{size * 100}"/></a:p>'
        )
    anchor = {"top": "t", "mid": "ctr", "bottom": "b"}.get(valign, "t")
    return f"""
<p:sp>
  <p:nvSpPr><p:cNvPr id="{sid}" name="TextBox {sid}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
    <a:noFill/><a:ln><a:noFill/></a:ln>
  </p:spPr>
  <p:txBody><a:bodyPr wrap="square" anchor="{anchor}"/><a:lstStyle/>{''.join(paras)}</p:txBody>
</p:sp>"""


def image_pic(sid: int, rid: str, name: str, x: float, y: float, w: float, h: float) -> str:
    return f"""
<p:pic>
  <p:nvPicPr><p:cNvPr id="{sid}" name="{xesc(name)}"/><p:cNvPicPr/><p:nvPr/></p:nvPicPr>
  <p:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>
  <p:spPr>
    <a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
    {line_xml("E2E8F0", 9525)}
  </p:spPr>
</p:pic>"""


def bullet_items(start_id: int, x: float, y: float, w: float, items: list[str], color="0F172A") -> tuple[str, int]:
    xml = []
    sid = start_id
    yy = y
    for item in items:
        xml.append(shape(sid, x, yy + 0.08, 0.11, 0.11, COLORS["green"], COLORS["green"], "ellipse"))
        sid += 1
        xml.append(text_box(sid, x + 0.28, yy, w - 0.28, 0.38, item, 18, color))
        sid += 1
        yy += 0.48
    return "".join(xml), sid


def header(title: str, subtitle: str | None = None) -> str:
    xml = text_box(2, 0.55, 0.28, 10.8, 0.45, title, 25, COLORS["ink"], True)
    if subtitle:
        xml += text_box(3, 0.57, 0.78, 10.5, 0.32, subtitle, 12, COLORS["muted"])
    xml += shape(4, 0.55, 1.12, 1.15, 0.05, COLORS["blue"], COLORS["blue"], "rect")
    return xml


def slide_xml(content: str) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
       xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr>{solid_fill(COLORS["bg"])}<a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      {content}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""


def rels_xml(rels: list[tuple[str, str, str]]) -> str:
    body = "".join(
        f'<Relationship Id="{rid}" Type="{typ}" Target="{xesc(target)}"/>' for rid, typ, target in rels
    )
    return f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">{body}</Relationships>'


def add_card(sid: int, x: float, y: float, w: float, h: float, title: str, body: str, fill: str, accent: str) -> str:
    return (
        shape(sid, x, y, w, h, fill, "E2E8F0")
        + shape(sid + 1, x, y, 0.08, h, accent, accent, "rect")
        + text_box(sid + 2, x + 0.25, y + 0.18, w - 0.45, 0.35, title, 16, COLORS["ink"], True)
        + text_box(sid + 3, x + 0.25, y + 0.62, w - 0.45, h - 0.7, body, 13, COLORS["muted"])
    )


slides: list[dict] = []


def add_slide(content: str, images: list[tuple[str, Path]] | None = None):
    slides.append({"content": content, "images": images or []})


# 1. Cover
content = (
    shape(10, 0, 0, 13.333, 7.5, COLORS["bg"], COLORS["bg"], "rect")
    + shape(11, 0, 0, 13.333, 0.18, COLORS["blue"], COLORS["blue"], "rect")
    + text_box(12, 0.72, 1.25, 11.6, 1.15, "Xây dựng hệ thống truy xuất nguồn gốc hàng hóa\nsử dụng công nghệ blockchain và microservice", 30, COLORS["ink"], True)
    + text_box(13, 0.75, 2.78, 8.6, 0.35, "Đồ án tốt nghiệp", 18, COLORS["blue"], True)
    + add_card(14, 0.78, 3.45, 5.8, 1.55, "Sinh viên thực hiện", "Nguyễn Đăng Nam\nMã SV: CT060226 | Lớp: CT6B", COLORS["white"], COLORS["green"])
    + add_card(18, 6.85, 3.45, 5.55, 1.55, "Giảng viên hướng dẫn", "TS. Trần Anh Tú\nThS. Nguyễn Văn Quyết", COLORS["white"], COLORS["blue"])
    + text_box(22, 0.78, 6.75, 8.5, 0.3, "Hệ thống truy xuất nguồn gốc | Blockchain | Microservices", 12, COLORS["muted"])
)
if (ASSETS / "image1.png").exists():
    content += image_pic(23, "rId1", "KMA", 11.35, 0.55, 1.15, 1.22)
    add_slide(content, [("rId1", ASSETS / "image1.png")])
else:
    add_slide(content)

# 2. Problem
bul, _ = bullet_items(20, 0.95, 1.65, 5.7, [
    "Người tiêu dùng khó kiểm chứng nguồn gốc thật của sản phẩm.",
    "Thông tin trên bao bì và mã truy xuất có thể bị sao chép.",
    "Doanh nghiệp cần quản lý minh bạch từ nguyên liệu đến bán lẻ.",
    "Dữ liệu truy xuất cần có cơ chế xác minh khi nghi ngờ bị sửa đổi.",
])
content = header("Bài toán và lý do chọn đề tài", "Từ nhu cầu thực tế của người mua và quản lý chuỗi cung ứng")
content += bul
content += add_card(35, 7.05, 1.58, 5.0, 3.3, "Vấn đề cốt lõi", "Làm sao để người dùng tại điểm mua có thể biết sản phẩm đến từ đâu, có đáng tin hay không, và dữ liệu truy xuất có bị can thiệp không?", COLORS["blue2"], COLORS["blue"])
content += add_card(39, 7.05, 5.1, 5.0, 1.1, "Hướng tiếp cận", "Kết hợp QR/serial, mobile app, microservices và blockchain lưu hash kiểm chứng.", COLORS["green2"], COLORS["green"])
add_slide(content)

# 3. Goals
content = header("Mục tiêu và ý nghĩa thực tiễn", "Tập trung vào trải nghiệm người dùng cuối và khả năng kiểm chứng dữ liệu")
content += add_card(20, 0.8, 1.55, 3.8, 1.55, "Người tiêu dùng", "Quét QR hoặc nhập serial để xem nguồn gốc, lịch sử vận chuyển và thông tin chi tiết sản phẩm.", COLORS["white"], COLORS["green"])
content += add_card(24, 4.8, 1.55, 3.8, 1.55, "Tham khảo đánh giá", "Xem đánh giá từ người đã mua, có thêm cơ sở trước khi quyết định mua hàng.", COLORS["white"], COLORS["blue"])
content += add_card(28, 8.8, 1.55, 3.8, 1.55, "Chống giả", "Serial tờ tiền được đăng ký và gắn duy nhất với từng sản phẩm để phát hiện dùng lại hoặc không khớp.", COLORS["white"], COLORS["orange"])
content += add_card(32, 0.8, 3.6, 5.8, 1.45, "Doanh nghiệp", "Theo dõi chuỗi nguyên liệu, sản xuất, đóng gói, đơn hàng và vận chuyển theo từng vai trò.", COLORS["green2"], COLORS["green"])
content += add_card(36, 6.85, 3.6, 5.75, 1.45, "Dữ liệu", "Hash dữ liệu quan trọng được neo lên blockchain, hỗ trợ phát hiện thay đổi bất thường.", COLORS["blue2"], COLORS["blue"])
content += text_box(40, 0.82, 6.15, 11.5, 0.45, "Mục tiêu chính: xây dựng hệ thống truy xuất nguồn gốc có thể dùng được, có kiểm chứng và phù hợp với bối cảnh đồ án.", 18, COLORS["ink"], True, "ctr")
add_slide(content)

# 4. Scope and actors
content = header("Phạm vi và tác nhân hệ thống", "Hệ thống có nhiều vai trò, mỗi vai trò đảm nhiệm một phần trong chuỗi cung ứng")
actors = [
    ("Admin", "Duyệt vai trò,\nquản lý hệ thống", COLORS["blue"]),
    ("Supplier", "Quản lý nguyên liệu,\ntạo raw batch", COLORS["green"]),
    ("Manufacturer", "Sản xuất, đóng gói,\nđăng ký serial", COLORS["orange"]),
    ("Retailer", "Đặt hàng\nthành phẩm", COLORS["blue"]),
    ("Transporter", "Xác nhận lấy hàng\nvà giao hàng", COLORS["green"]),
    ("User", "Quét truy xuất,\nxem/đánh giá SP", COLORS["orange"]),
]
x = 0.75
for idx, (name, body, accent) in enumerate(actors):
    cx = x + (idx % 3) * 4.15
    cy = 1.65 + (idx // 3) * 2.05
    content += add_card(20 + idx * 4, cx, cy, 3.65, 1.55, name, body, COLORS["white"], accent)
content += text_box(50, 0.82, 6.05, 11.8, 0.5, "Phạm vi triển khai: Web quản trị/nghiệp vụ, Mobile cho quét và tác vụ hiện trường, backend microservices và smart contract.", 16, COLORS["muted"], False, "ctr")
add_slide(content)

# 5. Architecture
content = header("Kiến trúc tổng thể", "Web/Mobile đi qua API Gateway, các service phối hợp bằng REST, Kafka và blockchain")
if (ASSETS / "image29.png").exists():
    content += image_pic(20, "rId1", "Architecture", 0.75, 1.4, 8.0, 4.5)
    content += add_card(21, 9.05, 1.45, 3.55, 1.2, "API Gateway", "Điểm vào chung, route request và kiểm tra token.", COLORS["white"], COLORS["blue"])
    content += add_card(25, 9.05, 2.9, 3.55, 1.2, "Microservices", "Identity, Catalog, Core, Trade Logistics, Blockchain.", COLORS["white"], COLORS["green"])
    content += add_card(29, 9.05, 4.35, 3.55, 1.2, "Bất đồng bộ", "Kafka xử lý ghi blockchain và cập nhật trạng thái.", COLORS["white"], COLORS["orange"])
    add_slide(content, [("rId1", ASSETS / "image29.png")])
else:
    add_slide(content)

# 6. Tech stack
content = header("Công nghệ sử dụng", "Lựa chọn công nghệ theo từng lớp của hệ thống")
groups = [
    ("Backend", "Spring Boot\nSpring Security\nEureka, Feign", COLORS["blue"], COLORS["blue2"]),
    ("Web", "React, Vite\nAnt Design\nReact Query", COLORS["green"], COLORS["green2"]),
    ("Mobile", "Flutter, Bloc\nDio\nQR/OCR scanner", COLORS["orange"], COLORS["orange2"]),
    ("Blockchain & Hạ tầng", "Solidity, Web3j\nPostgreSQL, Kafka\nWebSocket, Ganache", COLORS["blue"], COLORS["blue2"]),
]
for i, (title, body, accent, fill) in enumerate(groups):
    content += add_card(20 + i * 4, 0.85 + i * 3.05, 1.75, 2.72, 3.5, title, body, fill, accent)
content += text_box(45, 0.9, 5.9, 11.6, 0.45, "Điểm nhấn kỹ thuật: chia service theo trách nhiệm, xử lý blockchain bất đồng bộ, mobile hỗ trợ quét và xác minh tại hiện trường.", 16, COLORS["ink"], True, "ctr")
add_slide(content)

# 7. Business flow
content = header("Luồng nghiệp vụ chính", "Từ nguyên liệu đầu vào đến người dùng quét sản phẩm")
steps = [
    ("1", "Supplier\ntạo nguyên liệu"),
    ("2", "Manufacturer\nmua nguyên liệu"),
    ("3", "Sản xuất\npallet"),
    ("4", "Đóng gói\ncarton/unit"),
    ("5", "Retailer\nđặt hàng"),
    ("6", "Transporter\ngiao hàng"),
    ("7", "User\nquét truy xuất"),
]
for i, (num, label) in enumerate(steps):
    xx = 0.55 + i * 1.78
    content += shape(20 + i * 3, xx, 2.3, 0.72, 0.72, COLORS["blue"] if i < 4 else COLORS["green"], COLORS["white"], "ellipse")
    content += text_box(21 + i * 3, xx, 2.43, 0.72, 0.25, num, 18, COLORS["white"], True, "ctr")
    content += text_box(22 + i * 3, xx - 0.35, 3.2, 1.45, 0.75, label, 14, COLORS["ink"], True, "ctr")
    if i < len(steps) - 1:
        content += shape(60 + i, xx + 0.82, 2.62, 0.82, 0.08, COLORS["line"], COLORS["line"], "rect")
content += add_card(80, 1.15, 5.0, 11.0, 1.0, "Ý nghĩa của luồng", "Hệ thống mô phỏng được chuỗi cung ứng nhiều vai trò, nhiều trạng thái, không chỉ là quản lý dữ liệu đơn lẻ.", COLORS["white"], COLORS["green"])
add_slide(content)

# 8. Trace model
content = header("Mô hình truy xuất nguồn gốc", "Truy ngược từ sản phẩm cuối cùng về nguyên liệu đầu vào")
nodes = [
    ("Raw Batch", "Lô nguyên liệu"),
    ("Pallet", "Lô sản xuất"),
    ("Carton", "Thùng hàng"),
    ("Product Unit", "Sản phẩm lẻ"),
    ("QR / Serial", "Mã truy xuất"),
    ("User Scan", "Người dùng quét"),
]
for i, (title, sub) in enumerate(nodes):
    xx = 0.65 + i * 2.08
    fill = COLORS["green2"] if i < 2 else COLORS["blue2"] if i < 4 else COLORS["orange2"]
    accent = COLORS["green"] if i < 2 else COLORS["blue"] if i < 4 else COLORS["orange"]
    content += shape(20 + i * 4, xx, 2.0, 1.48, 1.05, fill, "E2E8F0")
    content += text_box(21 + i * 4, xx + 0.08, 2.18, 1.32, 0.3, title, 13, COLORS["ink"], True, "ctr")
    content += text_box(22 + i * 4, xx + 0.08, 2.55, 1.32, 0.25, sub, 10, COLORS["muted"], False, "ctr")
    if i < len(nodes) - 1:
        content += shape(23 + i * 4, xx + 1.52, 2.5, 0.48, 0.06, accent, accent, "rect")
content += add_card(60, 0.85, 4.35, 5.65, 1.25, "Khi truy xuất", "Mobile gọi API để tổng hợp product, carton, pallet, raw batch và lịch sử chuyển giao thành timeline.", COLORS["white"], COLORS["blue"])
content += add_card(64, 6.8, 4.35, 5.65, 1.25, "Khi xác minh", "Hệ thống tính lại hash từ dữ liệu hiện tại rồi so với hash đã ghi trên blockchain.", COLORS["white"], COLORS["green"])
add_slide(content)

# 9. Blockchain verification
content = header("Cơ chế blockchain và xác minh dữ liệu", "Blockchain không lưu toàn bộ dữ liệu, chỉ lưu hash để kiểm chứng")
flow = [
    ("PostgreSQL", "Lưu dữ liệu nghiệp vụ đầy đủ"),
    ("Keccak-256", "Tính hash dữ liệu quan trọng"),
    ("Blockchain", "Lưu hash và event audit"),
    ("Verify", "Tính lại hash và so sánh"),
]
for i, (title, sub) in enumerate(flow):
    xx = 0.9 + i * 3.05
    content += add_card(20 + i * 4, xx, 1.9, 2.45, 1.35, title, sub, COLORS["white"], COLORS["blue"] if i != 2 else COLORS["green"])
    if i < len(flow) - 1:
        content += shape(60 + i, xx + 2.52, 2.55, 0.45, 0.06, COLORS["line"], COLORS["line"], "rect")
content += add_card(75, 1.05, 4.25, 5.4, 1.25, "Dữ liệu hợp lệ", "Hash tính lại từ DB khớp với hash đã neo trên blockchain.", COLORS["green2"], COLORS["green"])
content += add_card(79, 6.85, 4.25, 5.4, 1.25, "Dữ liệu bị sửa", "Hash không khớp, hệ thống có thể cảnh báo dữ liệu nguồn gốc không còn toàn vẹn.", COLORS["red2"], COLORS["red"])
add_slide(content)

# 10. Anti-counterfeit and reviews
content = header("Chống giả và đánh giá sản phẩm", "Hai chức năng gần với người dùng cuối nhất")
content += add_card(20, 0.85, 1.65, 5.7, 2.15, "Chống giả bằng serial tờ tiền", "Mỗi sản phẩm được gắn với một serial tờ tiền đã đăng ký; serial chỉ được dùng một lần.\nNếu serial bị dùng lại hoặc không khớp sản phẩm, hệ thống phát hiện dấu hiệu bất thường.", COLORS["orange2"], COLORS["orange"])
content += add_card(24, 6.85, 1.65, 5.7, 2.15, "Đánh giá sản phẩm", "Người dùng đã mua có thể gửi đánh giá và hình ảnh.\nNgười mua sau có thêm nguồn tham khảo thực tế trước khi quyết định mua.", COLORS["green2"], COLORS["green"])
content += add_card(28, 1.15, 4.55, 10.95, 1.15, "Giá trị thực tiễn", "Khi đứng trước một sản phẩm tại siêu thị, người dùng không chỉ thấy thông tin trên bao bì mà còn xem được nguồn gốc, lịch sử, đánh giá và dấu hiệu xác thực.", COLORS["white"], COLORS["blue"])
add_slide(content)

# 11. Results / screenshots
content = header("Kết quả đạt được", "Một số màn hình và chức năng chính đã triển khai")
imgs = []
positions = [
    ("image41.png", 0.7, 1.45, 3.9, 1.35),
    ("image40.png", 4.85, 1.45, 3.9, 1.85),
    ("image58.png", 9.25, 1.35, 1.35, 3.0),
    ("image54.png", 10.85, 1.35, 1.35, 3.0),
]
for idx, (fn, x, y, w, h) in enumerate(positions, start=1):
    p = ASSETS / fn
    if p.exists():
        content += image_pic(20 + idx, f"rId{idx}", fn, x, y, w, h)
        imgs.append((f"rId{idx}", p))
content += text_box(35, 0.82, 4.85, 11.7, 0.35, "Đã xây dựng backend microservices, web nghiệp vụ, mobile quét truy xuất, tích hợp blockchain, Kafka và WebSocket.", 17, COLORS["ink"], True, "ctr")
bul, _ = bullet_items(40, 1.0, 5.45, 11.2, [
    "Web: admin, supplier, manufacturer, dashboard nghiệp vụ.",
    "Mobile: scan QR/serial, truy xuất timeline, retailer/transporter, đánh giá sản phẩm.",
    "Blockchain: ghi hash, verify dữ liệu, thống kê gas sử dụng.",
], COLORS["muted"])
content += bul
add_slide(content, imgs)

# 12. Conclusion
content = header("Đánh giá và kết luận", "Tổng hợp ngắn gọn cho phần kết thúc thuyết trình")
content += add_card(20, 0.85, 1.55, 3.75, 2.0, "Ưu điểm", "Có tính thực tiễn, nhiều vai trò, luồng nghiệp vụ tương đối đầy đủ.\nBlockchain được dùng hợp lý để xác minh dữ liệu.", COLORS["green2"], COLORS["green"])
content += add_card(24, 4.85, 1.55, 3.75, 2.0, "Hạn chế", "Môi trường còn mang tính demo local.\nKiểm thử tự động và bảo mật cấu hình cần hoàn thiện thêm.", COLORS["orange2"], COLORS["orange"])
content += add_card(28, 8.85, 1.55, 3.75, 2.0, "Hướng phát triển", "Triển khai cloud, quản lý secret chuẩn hơn, mở rộng dashboard phân tích và kiểm thử tích hợp.", COLORS["blue2"], COLORS["blue"])
content += text_box(34, 1.0, 4.65, 11.3, 0.9, "Kết luận: Hệ thống đáp ứng mục tiêu truy xuất nguồn gốc hàng hóa, minh bạch dữ liệu chuỗi cung ứng và hỗ trợ chống giả sản phẩm.", 23, COLORS["ink"], True, "ctr", "mid")
content += text_box(35, 1.0, 6.25, 11.3, 0.4, "Em xin chân thành cảm ơn!", 20, COLORS["blue"], True, "ctr")
add_slide(content)


def build_package():
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
        # Content types
        overrides = [
            '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>',
            '<Default Extension="xml" ContentType="application/xml"/>',
            '<Default Extension="png" ContentType="image/png"/>',
            '<Default Extension="jpg" ContentType="image/jpeg"/>',
            '<Default Extension="jpeg" ContentType="image/jpeg"/>',
            '<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>',
            '<Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>',
            '<Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>',
            '<Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>',
            '<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>',
            '<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>',
        ]
        for i in range(1, len(slides) + 1):
            overrides.append(f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>')
        z.writestr("[Content_Types].xml", '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' + "".join(overrides) + "</Types>")

        z.writestr("_rels/.rels", rels_xml([
            ("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument", "ppt/presentation.xml"),
            ("rId2", "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties", "docProps/core.xml"),
            ("rId3", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties", "docProps/app.xml"),
        ]))

        z.writestr("docProps/core.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:title>DATN - Truy xuất nguồn gốc blockchain</dc:title><dc:creator>Nguyễn Đăng Nam</dc:creator><cp:lastModifiedBy>Codex</cp:lastModifiedBy></cp:coreProperties>""")
        z.writestr("docProps/app.xml", f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Microsoft PowerPoint</Application><Slides>{len(slides)}</Slides></Properties>""")

        # Presentation
        sld_ids = "".join(f'<p:sldId id="{255+i}" r:id="rId{i+1}"/>' for i in range(1, len(slides) + 1))
        z.writestr("ppt/presentation.xml", f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst><p:sldIdLst>{sld_ids}</p:sldIdLst>
<p:sldSz cx="{SLIDE_W}" cy="{SLIDE_H}" type="wide"/><p:notesSz cx="6858000" cy="9144000"/><p:defaultTextStyle/></p:presentation>""")
        pres_rels = [("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster", "slideMasters/slideMaster1.xml")]
        for i in range(1, len(slides) + 1):
            pres_rels.append((f"rId{i+1}", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide", f"slides/slide{i}.xml"))
        z.writestr("ppt/_rels/presentation.xml.rels", rels_xml(pres_rels))

        # Minimal master/layout/theme
        z.writestr("ppt/slideMasters/slideMaster1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/><p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId2"/></p:sldLayoutIdLst><p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles></p:sldMaster>""")
        z.writestr("ppt/slideMasters/_rels/slideMaster1.xml.rels", rels_xml([
            ("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme", "../theme/theme1.xml"),
            ("rId2", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout", "../slideLayouts/slideLayout1.xml"),
        ]))
        z.writestr("ppt/slideLayouts/slideLayout1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1"><p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>""")
        z.writestr("ppt/slideLayouts/_rels/slideLayout1.xml.rels", rels_xml([
            ("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster", "../slideMasters/slideMaster1.xml")
        ]))
        z.writestr("ppt/theme/theme1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="DATN Theme"><a:themeElements><a:clrScheme name="DATN"><a:dk1><a:srgbClr val="0F172A"/></a:dk1><a:lt1><a:srgbClr val="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="1F2937"/></a:dk2><a:lt2><a:srgbClr val="F8FAFC"/></a:lt2><a:accent1><a:srgbClr val="2563EB"/></a:accent1><a:accent2><a:srgbClr val="16A34A"/></a:accent2><a:accent3><a:srgbClr val="F97316"/></a:accent3><a:accent4><a:srgbClr val="DC2626"/></a:accent4><a:accent5><a:srgbClr val="64748B"/></a:accent5><a:accent6><a:srgbClr val="0F172A"/></a:accent6><a:hlink><a:srgbClr val="2563EB"/></a:hlink><a:folHlink><a:srgbClr val="1D4ED8"/></a:folHlink></a:clrScheme><a:fontScheme name="Aptos"><a:majorFont><a:latin typeface="Aptos Display"/></a:majorFont><a:minorFont><a:latin typeface="Aptos"/></a:minorFont></a:fontScheme><a:fmtScheme name="Office"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="9525"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>""")

        media_counter = 1
        for i, s in enumerate(slides, start=1):
            z.writestr(f"ppt/slides/slide{i}.xml", slide_xml(s["content"]))
            rels = [("rIdLayout", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout", "../slideLayouts/slideLayout1.xml")]
            for rid, path in s["images"]:
                ext = path.suffix.lower().lstrip(".")
                target = f"../media/media{media_counter}.{ext}"
                z.write(path, f"ppt/media/media{media_counter}.{ext}")
                rels.append((rid, "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image", target))
                media_counter += 1
            z.writestr(f"ppt/slides/_rels/slide{i}.xml.rels", rels_xml(rels))


if __name__ == "__main__":
    build_package()
    print(OUT)

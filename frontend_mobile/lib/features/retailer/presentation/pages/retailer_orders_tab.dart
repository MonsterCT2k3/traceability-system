import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/network/api_client.dart';

class RetailerOrdersTab extends StatefulWidget {
  const RetailerOrdersTab({super.key});

  @override
  State<RetailerOrdersTab> createState() => _RetailerOrdersTabState();
}

class _RetailerOrdersTabState extends State<RetailerOrdersTab> {
  final ApiClient _api = GetIt.I<ApiClient>();

  final _formKey = GlobalKey<FormState>();
  final TextEditingController _cartonController = TextEditingController();
  final TextEditingController _noteController = TextEditingController();

  List<_DirectoryUser> _manufacturers = [];
  List<_ProductItem> _products = [];
  List<_TradeOrderLite> _myOrders = [];

  String? _selectedManufacturerId;
  String? _selectedProductId;

  bool _loadingMeta = true;
  bool _submitting = false;
  bool _loadingOrders = false;
  String? _errorMeta;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  @override
  void dispose() {
    _cartonController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  Future<void> _bootstrap() async {
    setState(() {
      _loadingMeta = true;
      _errorMeta = null;
    });
    try {
      final productsRes = await _api.get('/product/api/v1/products');
      final productsRaw = productsRes.data;
      final products = <_ProductItem>[];
      if (productsRaw is Map && productsRaw['result'] is List) {
        for (final e in productsRaw['result'] as List) {
          products.add(_ProductItem.fromJson(Map<String, dynamic>.from(e as Map)));
        }
      }

      final manufacturers = await _buildManufacturersFromProducts(products);

      setState(() {
        _manufacturers = manufacturers;
        _products = products;
        _loadingMeta = false;
      });
      await _fetchMyOrders();
    } on DioException catch (e) {
      setState(() {
        _loadingMeta = false;
        _errorMeta = _readApiMessage(e) ?? 'Không tải được dữ liệu đặt hàng';
      });
    } catch (e) {
      setState(() {
        _loadingMeta = false;
        _errorMeta = e.toString();
      });
    }
  }

  Future<List<_DirectoryUser>> _buildManufacturersFromProducts(List<_ProductItem> products) async {
    final ownerIds = products
        .map((e) => e.ownerId?.trim() ?? '')
        .where((id) => id.isNotEmpty)
        .toSet()
        .toList()
      ..sort();

    final out = <_DirectoryUser>[];
    for (final id in ownerIds) {
      try {
        final res = await _api.get('/identity/api/v1/users/directory/by-id/${Uri.encodeComponent(id)}');
        final raw = res.data;
        if (raw is Map && raw['result'] is Map) {
          out.add(_DirectoryUser.fromJson(Map<String, dynamic>.from(raw['result'] as Map)));
          continue;
        }
      } catch (_) {
        // fallback id-only khi directory không resolve được tên.
      }
      out.add(_DirectoryUser(id: id));
    }
    return out;
  }

  Future<void> _fetchMyOrders() async {
    setState(() => _loadingOrders = true);
    try {
      final res = await _api.get('/product/api/v1/orders/mine/buyer');
      final raw = res.data;
      final list = <_TradeOrderLite>[];
      if (raw is Map && raw['result'] is List) {
        for (final e in raw['result'] as List) {
          final one = _TradeOrderLite.fromJson(Map<String, dynamic>.from(e as Map));
          if (one.orderType == 'RETAILER_TO_MANUFACTURER') {
            list.add(one);
          }
        }
      }
      setState(() {
        _myOrders = list;
        _loadingOrders = false;
      });
    } on DioException catch (e) {
      setState(() => _loadingOrders = false);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(_readApiMessage(e) ?? 'Không tải được danh sách đơn'),
          backgroundColor: Colors.redAccent,
        ),
      );
    } catch (_) {
      setState(() => _loadingOrders = false);
    }
  }

  List<_ProductItem> get _filteredProducts {
    final seller = _selectedManufacturerId;
    if (seller == null || seller.isEmpty) return const [];
    return _products.where((p) => p.ownerId == seller).toList();
  }

  String _displayProductName(_TradeOrderLite order) {
    final inline = (order.productName ?? '').trim();
    if (inline.isNotEmpty) return inline;

    final pid = (order.productId ?? '').trim();
    if (pid.isEmpty) return '—';

    for (final p in _products) {
      if (p.id == pid) return p.name;
    }
    return pid;
  }

  Future<void> _submitOrder() async {
    if (!_formKey.currentState!.validate()) return;
    final sellerId = _selectedManufacturerId;
    final productId = _selectedProductId;
    if (sellerId == null || productId == null) return;

    final cartons = int.tryParse(_cartonController.text.trim());
    if (cartons == null || cartons <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Số thùng phải là số dương'),
          backgroundColor: Colors.redAccent,
        ),
      );
      return;
    }

    setState(() => _submitting = true);
    try {
      final payload = {
        'orderType': 'RETAILER_TO_MANUFACTURER',
        'sellerId': sellerId,
        'note': _noteController.text.trim().isEmpty ? null : _noteController.text.trim(),
        'lines': [
          {
            'productId': productId,
            'quantityCartons': cartons,
          }
        ],
      };
      await _api.post('/product/api/v1/orders', data: payload);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đặt hàng thành công')),
      );
      _cartonController.clear();
      _noteController.clear();
      await _fetchMyOrders();
    } on DioException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(_readApiMessage(e) ?? 'Đặt hàng thất bại'),
          backgroundColor: Colors.redAccent,
        ),
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loadingMeta) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_errorMeta != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error_outline, size: 48, color: Colors.red.shade300),
              const SizedBox(height: 10),
              Text(_errorMeta!, textAlign: TextAlign.center),
              const SizedBox(height: 14),
              FilledButton(onPressed: _bootstrap, child: const Text('Thử lại')),
            ],
          ),
        ),
      );
    }

    final filteredProducts = _filteredProducts;

    return RefreshIndicator(
      onRefresh: _bootstrap,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 80),
        children: [
          Card(
            elevation: 0,
            color: Colors.blue.shade50,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Tạo đơn đặt hàng',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String>(
                      value: _selectedManufacturerId,
                      decoration: const InputDecoration(
                        labelText: 'Nhà sản xuất',
                        border: OutlineInputBorder(),
                      ),
                      isExpanded: true,
                      items: _manufacturers
                          .map(
                            (m) => DropdownMenuItem<String>(
                              value: m.id,
                              child: Text(m.displayName),
                            ),
                          )
                          .toList(),
                      onChanged: (v) {
                        setState(() {
                          _selectedManufacturerId = v;
                          _selectedProductId = null;
                        });
                      },
                      validator: (v) => (v == null || v.isEmpty) ? 'Chọn nhà sản xuất' : null,
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String>(
                      value: _selectedProductId,
                      decoration: const InputDecoration(
                        labelText: 'Sản phẩm',
                        border: OutlineInputBorder(),
                      ),
                      isExpanded: true,
                      items: filteredProducts
                          .map(
                            (p) => DropdownMenuItem<String>(
                              value: p.id,
                              child: Text('${p.name}${p.price != null ? ' (${_formatMoney(p.price!)})' : ''}'),
                            ),
                          )
                          .toList(),
                      onChanged: _selectedManufacturerId == null
                          ? null
                          : (v) => setState(() => _selectedProductId = v),
                      validator: (v) => (v == null || v.isEmpty) ? 'Chọn sản phẩm' : null,
                    ),
                    if (_selectedManufacturerId != null && filteredProducts.isEmpty) ...[
                      const SizedBox(height: 8),
                      const Text(
                        'Nhà sản xuất này chưa có sản phẩm để đặt.',
                        style: TextStyle(color: Colors.redAccent, fontSize: 12),
                      ),
                    ],
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _cartonController,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(
                        labelText: 'Số thùng',
                        border: OutlineInputBorder(),
                      ),
                      validator: (v) {
                        final n = int.tryParse((v ?? '').trim());
                        if (n == null || n <= 0) return 'Nhập số thùng hợp lệ';
                        return null;
                      },
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _noteController,
                      maxLines: 2,
                      decoration: const InputDecoration(
                        labelText: 'Ghi chú (tùy chọn)',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: _submitting ? null : _submitOrder,
                        icon: _submitting
                            ? const SizedBox(
                                width: 16,
                                height: 16,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : const Icon(Icons.send_outlined),
                        label: const Text('Đặt hàng'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Đơn đã đặt',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
              if (_loadingOrders)
                const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
            ],
          ),
          const SizedBox(height: 8),
          if (_myOrders.isEmpty)
            Card(
              elevation: 0,
              color: Colors.grey.shade100,
              child: const Padding(
                padding: EdgeInsets.all(14),
                child: Text('Chưa có đơn RETAILER_TO_MANUFACTURER nào.'),
              ),
            )
          else
            ..._myOrders.map(
              (o) => Card(
                elevation: 0,
                color: Colors.grey.shade50,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: Colors.teal.shade100,
                    child: Icon(Icons.receipt_long, color: Colors.teal.shade700),
                  ),
                  title: Text(
                    o.orderCode,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  subtitle: Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      'Trạng thái: ${_statusLabel(o.status)}\n'
                      'Sản phẩm: ${_displayProductName(o)} · ${o.quantityCartons ?? 0} thùng',
                    ),
                  ),
                  isThreeLine: true,
                ),
              ),
            ),
        ],
      ),
    );
  }

  static String? _readApiMessage(DioException e) {
    final data = e.response?.data;
    if (data is Map && data['message'] != null) {
      return '${data['message']}';
    }
    return e.message;
  }

  static String _statusLabel(String s) {
    switch (s) {
      case 'PENDING':
        return 'Chờ xử lý';
      case 'ACCEPTED':
        return 'Đã chấp nhận';
      case 'REJECTED':
        return 'Từ chối';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'ASSIGNED_TO_CARRIER':
        return 'Đã gán vận chuyển';
      case 'PICKED_UP_FROM_SELLER':
        return 'Đang giao';
      case 'DELIVERED':
        return 'Đã giao';
      default:
        return s;
    }
  }

  static String _formatMoney(double value) {
    final fixed = value == value.roundToDouble()
        ? value.toStringAsFixed(0)
        : value.toStringAsFixed(2);
    return '$fixed đ';
  }
}

class _DirectoryUser {
  final String id;
  final String? username;
  final String? fullName;

  _DirectoryUser({
    required this.id,
    this.username,
    this.fullName,
  });

  factory _DirectoryUser.fromJson(Map<String, dynamic> json) {
    return _DirectoryUser(
      id: json['id']?.toString() ?? '',
      username: json['username']?.toString(),
      fullName: json['fullName']?.toString(),
    );
  }

  String get displayName {
    final f = fullName?.trim() ?? '';
    final u = username?.trim() ?? '';
    if (f.isNotEmpty && u.isNotEmpty) return '$f (@$u)';
    if (f.isNotEmpty) return f;
    if (u.isNotEmpty) return '@$u';
    return id;
  }
}

class _ProductItem {
  final String id;
  final String name;
  final String? ownerId;
  final double? price;

  _ProductItem({
    required this.id,
    required this.name,
    this.ownerId,
    this.price,
  });

  factory _ProductItem.fromJson(Map<String, dynamic> json) {
    return _ProductItem(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? 'Không tên',
      ownerId: json['ownerId']?.toString(),
      price: (json['price'] is num) ? (json['price'] as num).toDouble() : double.tryParse('${json['price']}'),
    );
  }
}

class _TradeOrderLite {
  final String id;
  final String orderCode;
  final String status;
  final String orderType;
  final String? productId;
  final String? productName;
  final int? quantityCartons;

  _TradeOrderLite({
    required this.id,
    required this.orderCode,
    required this.status,
    required this.orderType,
    this.productId,
    this.productName,
    this.quantityCartons,
  });

  factory _TradeOrderLite.fromJson(Map<String, dynamic> json) {
    final lines = json['lines'];
    Map<String, dynamic>? firstLine;
    if (lines is List && lines.isNotEmpty) {
      firstLine = Map<String, dynamic>.from(lines.first as Map);
    }
    final rawQty = firstLine?['quantityCartons'];
    return _TradeOrderLite(
      id: json['id']?.toString() ?? '',
      orderCode: json['orderCode']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      orderType: json['orderType']?.toString() ?? '',
      productId: firstLine?['productId']?.toString(),
      productName: firstLine?['productName']?.toString(),
      quantityCartons: rawQty is int ? rawQty : int.tryParse('${rawQty ?? ''}'),
    );
  }
}

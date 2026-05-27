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
      final productsRes = await _api.get('/catalog/api/v1/products');
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

  Future<List<_DirectoryUser>> _buildManufacturersFromProducts(
    List<_ProductItem> products,
  ) async {
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
      } catch (_) {}
      out.add(_DirectoryUser(id: id));
    }
    return out;
  }

  Future<void> _fetchMyOrders() async {
    setState(() => _loadingOrders = true);
    try {
      final res = await _api.get('/trade/api/v1/orders/mine/buyer');
      final raw = res.data;
      final list = <_TradeOrderLite>[];
      if (raw is Map && raw['result'] is List) {
        for (final e in raw['result'] as List) {
          final one = _TradeOrderLite.fromJson(Map<String, dynamic>.from(e as Map));
          if (one.orderType == 'RETAILER_TO_MANUFACTURER') list.add(one);
        }
      }
      list.sort((a, b) => (b.createdAt ?? '').compareTo(a.createdAt ?? ''));
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
    if (pid.isEmpty) return '--';
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
          {'productId': productId, 'quantityCartons': cartons}
        ],
      };
      await _api.post('/trade/api/v1/orders', data: payload);
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
    if (_loadingMeta) return const Center(child: CircularProgressIndicator());

    if (_errorMeta != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.cloud_off_outlined, size: 52, color: Colors.red.shade300),
              const SizedBox(height: 10),
              const Text(
                'Không tải được dữ liệu đặt hàng',
                style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
              ),
              const SizedBox(height: 8),
              Text(_errorMeta!, textAlign: TextAlign.center),
              const SizedBox(height: 14),
              FilledButton.icon(
                onPressed: _bootstrap,
                icon: const Icon(Icons.refresh),
                label: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }

    return Stack(
      children: [
        RefreshIndicator(
          onRefresh: _bootstrap,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 90),
            children: [
              _buildSummary(),
              _buildCreateOrderForm(),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Đơn đã đặt',
                    style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
                  ),
                  if (_loadingOrders)
                    const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                ],
              ),
              const SizedBox(height: 10),
              if (_myOrders.isEmpty)
                const _EmptyState()
              else
                ..._myOrders.map(
                  (o) => Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _OrderCard(
                      orderCode: o.orderCode,
                      product: _displayProductName(o),
                      quantity: o.quantityCartons ?? 0,
                      status: _statusLabel(o.status),
                      statusColor: _statusColor(o.status),
                      statusIcon: _statusIcon(o.status),
                      nextStep: _nextStep(o.status),
                    ),
                  ),
                ),
            ],
          ),
        ),
        if (_loadingOrders)
          const Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: LinearProgressIndicator(minHeight: 2),
          ),
      ],
    );
  }

  Widget _buildSummary() {
    final all = _myOrders.length;
    final pending = _myOrders.where((e) => e.status == 'PENDING').length;
    final delivered = _myOrders.where((e) => e.status == 'DELIVERED').length;
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF0E7490), Color(0xFF059669)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.shopping_bag_outlined, color: Colors.white),
              SizedBox(width: 8),
              Text(
                'Đơn Hàng Retailer',
                style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w700),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              _SummaryPill(label: 'Tổng đơn', value: '$all'),
              const SizedBox(width: 10),
              _SummaryPill(label: 'Chờ xử lý', value: '$pending'),
              const SizedBox(width: 10),
              _SummaryPill(label: 'Đã giao', value: '$delivered'),
            ],
          ),
          const SizedBox(height: 12),
          const Text(
            'Kéo xuống để cập nhật danh sách đơn mới nhất',
            style: TextStyle(color: Colors.white70, fontSize: 12),
          ),
        ],
      ),
    );
  }

  Widget _buildCreateOrderForm() {
    final filteredProducts = _filteredProducts;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.blue.shade50, Colors.white],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: Colors.blue.shade100),
        boxShadow: [
          BoxShadow(
            color: Colors.blue.shade100.withOpacity(0.28),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: Colors.blue.shade100,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(Icons.add_shopping_cart, color: Colors.blue.shade800),
                ),
                const SizedBox(width: 10),
                const Text(
                  'Tạo Đơn Đặt Hàng',
                  style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              'Chọn nhà sản xuất, sản phẩm và số lượng thùng cần đặt.',
              style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
            ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              value: _selectedManufacturerId,
              isExpanded: true,
              decoration: const InputDecoration(
                labelText: 'Nhà sản xuất',
                border: OutlineInputBorder(),
              ),
              items: _manufacturers
                  .map(
                    (m) => DropdownMenuItem<String>(
                      value: m.id,
                      child: Text(
                        m.displayName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
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
              isExpanded: true,
              decoration: const InputDecoration(
                labelText: 'Sản phẩm',
                border: OutlineInputBorder(),
              ),
              items: filteredProducts
                  .map(
                    (p) => DropdownMenuItem<String>(
                      value: p.id,
                      child: Text(
                        '${p.name}${p.price != null ? ' (${_formatMoney(p.price!)})' : ''}',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
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
                style: FilledButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
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
    );
  }

  static String? _readApiMessage(DioException e) {
    final data = e.response?.data;
    if (data is Map && data['message'] != null) return '${data['message']}';
    return e.message;
  }

  static String _statusLabel(String status) {
    switch (status) {
      case 'PENDING':
        return 'Chờ xử lý';
      case 'PROCESSING':
        return 'Đang xử lý';
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
        return status;
    }
  }

  static Color _statusColor(String status) {
    switch (status) {
      case 'PENDING':
      case 'PROCESSING':
        return Colors.orange;
      case 'ACCEPTED':
        return Colors.blue;
      case 'REJECTED':
      case 'CANCELLED':
        return Colors.red;
      case 'ASSIGNED_TO_CARRIER':
        return Colors.deepPurple;
      case 'PICKED_UP_FROM_SELLER':
        return Colors.teal;
      case 'DELIVERED':
        return Colors.green;
      default:
        return Colors.blueGrey;
    }
  }

  static IconData _statusIcon(String status) {
    switch (status) {
      case 'PENDING':
      case 'PROCESSING':
        return Icons.hourglass_top_rounded;
      case 'ACCEPTED':
        return Icons.check_circle_outline_rounded;
      case 'ASSIGNED_TO_CARRIER':
        return Icons.local_shipping_outlined;
      case 'PICKED_UP_FROM_SELLER':
        return Icons.route_outlined;
      case 'DELIVERED':
        return Icons.task_alt_rounded;
      case 'REJECTED':
      case 'CANCELLED':
        return Icons.cancel_outlined;
      default:
        return Icons.receipt_long_outlined;
    }
  }

  static String _nextStep(String status) {
    switch (status) {
      case 'PENDING':
        return 'Nhà sản xuất đang chờ xử lý';
      case 'PROCESSING':
        return 'Đơn hàng đang được xử lý';
      case 'ACCEPTED':
        return 'Đơn đã được chấp nhận';
      case 'ASSIGNED_TO_CARRIER':
        return 'Đã gán đơn vị vận chuyển';
      case 'PICKED_UP_FROM_SELLER':
        return 'Hàng đang được giao';
      case 'DELIVERED':
        return 'Giao hàng thành công';
      default:
        return 'Theo dõi trạng thái đơn';
    }
  }

  static String _formatMoney(double value) {
    final fixed = value == value.roundToDouble() ? value.toStringAsFixed(0) : value.toStringAsFixed(2);
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
  final String? createdAt;
  final String? productId;
  final String? productName;
  final int? quantityCartons;

  _TradeOrderLite({
    required this.id,
    required this.orderCode,
    required this.status,
    required this.orderType,
    this.createdAt,
    this.productId,
    this.productName,
    this.quantityCartons,
  });

  factory _TradeOrderLite.fromJson(Map<String, dynamic> json) {
    final lines = json['lines'];
    Map<String, dynamic>? firstLine;
    if (lines is List && lines.isNotEmpty) firstLine = Map<String, dynamic>.from(lines.first as Map);
    final rawQty = firstLine?['quantityCartons'];
    return _TradeOrderLite(
      id: json['id']?.toString() ?? '',
      orderCode: json['orderCode']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      orderType: json['orderType']?.toString() ?? '',
      createdAt: json['createdAt']?.toString(),
      productId: firstLine?['productId']?.toString(),
      productName: firstLine?['productName']?.toString(),
      quantityCartons: rawQty is int ? rawQty : int.tryParse('${rawQty ?? ''}'),
    );
  }
}

class _SummaryPill extends StatelessWidget {
  const _SummaryPill({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.16),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              value,
              style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
            ),
            Text(label, style: const TextStyle(color: Colors.white70, fontSize: 12)),
          ],
        ),
      ),
    );
  }
}

class _OrderCard extends StatelessWidget {
  const _OrderCard({
    required this.orderCode,
    required this.product,
    required this.quantity,
    required this.status,
    required this.statusColor,
    required this.statusIcon,
    required this.nextStep,
  });

  final String orderCode;
  final String product;
  final int quantity;
  final String status;
  final Color statusColor;
  final IconData statusIcon;
  final String nextStep;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: Colors.grey.shade200),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    orderCode,
                    style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
                  ),
                ),
                _StatusBadge(label: status, color: statusColor),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                _MiniInfo(icon: Icons.inventory_2_outlined, value: product),
                const SizedBox(width: 14),
                _MiniInfo(icon: Icons.layers_outlined, value: '$quantity thùng'),
              ],
            ),
            const SizedBox(height: 12),
            Divider(height: 1, color: Colors.grey.shade200),
            const SizedBox(height: 10),
            Row(
              children: [
                Icon(statusIcon, color: statusColor, size: 19),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    nextStep,
                    style: TextStyle(color: Colors.grey.shade700, fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(30),
      ),
      child: Text(
        label,
        style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w700),
      ),
    );
  }
}

class _MiniInfo extends StatelessWidget {
  const _MiniInfo({required this.icon, required this.value});

  final IconData icon;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 17, color: Colors.grey.shade600),
        const SizedBox(width: 6),
        Text(value, style: TextStyle(color: Colors.grey.shade700, fontWeight: FontWeight.w500)),
      ],
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 54, horizontal: 24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        children: [
          Icon(Icons.inbox_outlined, size: 52, color: Colors.grey.shade300),
          const SizedBox(height: 10),
          const Text(
            'Chưa có đơn hàng',
            style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
          ),
          const SizedBox(height: 6),
          Text(
            'Tạo đơn retailer để quản lý và theo dõi trạng thái tại đây.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.grey.shade600),
          ),
        ],
      ),
    );
  }
}

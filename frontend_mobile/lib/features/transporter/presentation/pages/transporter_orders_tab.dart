import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/network/api_client.dart';
import '../../data/trade_order_models.dart';

/// Danh sách đơn được gán cho VC: nhận hàng tại NCC/NSX → xác nhận giao tới người mua.
class TransporterOrdersTab extends StatefulWidget {
  const TransporterOrdersTab({super.key});

  @override
  State<TransporterOrdersTab> createState() => _TransporterOrdersTabState();
}

class _TransporterOrdersTabState extends State<TransporterOrdersTab> {
  final ApiClient _api = GetIt.I<ApiClient>();
  List<TradeOrderDto> _orders = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final Response res = await _api.get('/product/api/v1/orders/mine/carrier');
      final raw = res.data;
      if (raw is Map && raw['result'] is List) {
        final list = (raw['result'] as List)
            .map((e) => TradeOrderDto.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList();
        setState(() {
          _orders = list;
          _loading = false;
        });
        return;
      }
      setState(() {
        _orders = [];
        _loading = false;
      });
    } on DioException catch (e) {
      setState(() {
        _error = e.response?.data is Map ? '${(e.response!.data as Map)['message']}' : e.message;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  /// Khớp logic `UserDirectoryDisplay` trên web: `Tên (uuid)` hoặc chỉ id khi lỗi.
  String _formatDirectoryUserLine(Map<String, dynamic>? u, String fallbackId) {
    if (u == null) return fallbackId;
    final fullName = u['fullName']?.toString().trim() ?? '';
    final username = u['username']?.toString().trim() ?? '';
    final name = fullName.isNotEmpty ? fullName : (username.isNotEmpty ? username : '');
    final id = u['id']?.toString() ?? fallbackId;
    if (name.isNotEmpty) return '$name ($id)';
    return '($id)';
  }

  Future<String> _fetchUserDirectoryLabel(String userId) async {
    if (userId.isEmpty) return '—';
    try {
      final Response res = await _api.get(
        '/identity/api/v1/users/directory/by-id/${Uri.encodeComponent(userId)}',
      );
      final raw = res.data;
      if (raw is Map && raw['result'] != null) {
        return _formatDirectoryUserLine(
          Map<String, dynamic>.from(raw['result'] as Map),
          userId,
        );
      }
    } catch (_) {}
    return userId;
  }

  String _statusLabel(String s) {
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
        return 'Chờ đến nhận hàng tại người bán';
      case 'PICKED_UP_FROM_SELLER':
        return 'Đang giao tới người mua';
      case 'DELIVERED':
        return 'Đã giao';
      default:
        return s;
    }
  }

  Future<void> _openDetail(TradeOrderDto order) async {
    final messenger = ScaffoldMessenger.of(context);
    TradeOrderDto detail = order;
    try {
      final Response res = await _api.get('/product/api/v1/orders/${order.id}');
      if (res.data is Map && (res.data as Map)['result'] != null) {
        detail = TradeOrderDto.fromJson(
          Map<String, dynamic>.from((res.data as Map)['result'] as Map),
        );
      }
    } catch (_) {
      // giữ bản list
    }

    String buyerLine = detail.buyerId;
    String sellerLine = detail.sellerId;
    try {
      final resolved = await Future.wait([
        _fetchUserDirectoryLabel(detail.buyerId),
        _fetchUserDirectoryLabel(detail.sellerId),
      ]);
      buyerLine = resolved[0];
      sellerLine = resolved[1];
    } catch (_) {}

    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (ctx) => DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.55,
        minChildSize: 0.35,
        maxChildSize: 0.92,
        builder: (_, scroll) => SingleChildScrollView(
          controller: scroll,
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  margin: const EdgeInsets.only(bottom: 16),
                  decoration: BoxDecoration(
                    color: Colors.grey.shade300,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              Text(
                detail.orderCode,
                style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text('Trạng thái: ${_statusLabel(detail.status)}'),
              const SizedBox(height: 8),
              Text('Loại: ${detail.orderType}', style: TextStyle(color: Colors.grey.shade700, fontSize: 13)),
              const SizedBox(height: 12),
              Text('Người mua: $buyerLine', style: const TextStyle(fontSize: 12)),
              Text('Người bán: $sellerLine', style: const TextStyle(fontSize: 12)),
              if (detail.note != null && detail.note!.trim().isNotEmpty) ...[
                const SizedBox(height: 12),
                Text('Ghi chú: ${detail.note}'),
              ],
              if (detail.lines.isNotEmpty) ...[
                const SizedBox(height: 16),
                const Text('Dòng lô:', style: TextStyle(fontWeight: FontWeight.w600)),
                ...detail.lines.map(
                  (l) => Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      '#${l.lineIndex ?? 0} · ${l.targetRawBatchId ?? '—'} · ${l.quantityRequested ?? ''} ${l.unit ?? ''}',
                      style: const TextStyle(fontSize: 12),
                    ),
                  ),
                ),
              ],
              if (detail.status == 'ASSIGNED_TO_CARRIER') ...[
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: () async {
                      final ok = await showDialog<bool>(
                        context: ctx,
                        builder: (dCtx) => AlertDialog(
                          title: const Text('Xác nhận đã nhận hàng?'),
                          content: const Text(
                            'Xác nhận bạn đã đến chỗ người bán và nhận hàng. Sau bước này bạn mới xác nhận giao tới người mua.',
                          ),
                          actions: [
                            TextButton(onPressed: () => Navigator.pop(dCtx, false), child: const Text('Hủy')),
                            FilledButton(onPressed: () => Navigator.pop(dCtx, true), child: const Text('Xác nhận')),
                          ],
                        ),
                      );
                      if (ok != true) return;
                      try {
                        await _api.post('/product/api/v1/orders/${detail.id}/confirm-picked-up');
                        if (!context.mounted) return;
                        if (ctx.mounted) Navigator.of(ctx).pop();
                        messenger.showSnackBar(
                          const SnackBar(content: Text('Đã xác nhận nhận hàng tại người bán')),
                        );
                        _fetch();
                      } on DioException catch (e) {
                        final msg = e.response?.data is Map
                            ? '${(e.response!.data as Map)['message']}'
                            : e.message;
                        if (!context.mounted) return;
                        messenger.showSnackBar(
                          SnackBar(content: Text(msg ?? 'Lỗi'), backgroundColor: Colors.redAccent),
                        );
                      }
                    },
                    icon: const Icon(Icons.inventory_2_outlined),
                    label: const Text('Xác nhận đã nhận hàng tại NCC/NSX'),
                  ),
                ),
              ],
              if (detail.status == 'PICKED_UP_FROM_SELLER') ...[
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: () async {
                      final ok = await showDialog<bool>(
                        context: ctx,
                        builder: (dCtx) => AlertDialog(
                          title: const Text('Xác nhận đã giao?'),
                          content: const Text(
                            'Xác nhận bạn đã giao hàng tới người mua. Hệ thống sẽ cập nhật đơn và (nếu có) ghi nhận blockchain.',
                          ),
                          actions: [
                            TextButton(onPressed: () => Navigator.pop(dCtx, false), child: const Text('Hủy')),
                            FilledButton(onPressed: () => Navigator.pop(dCtx, true), child: const Text('Xác nhận')),
                          ],
                        ),
                      );
                      if (ok != true) return;
                      try {
                        await _api.post('/product/api/v1/orders/${detail.id}/confirm-delivered');
                        if (!context.mounted) return;
                        if (ctx.mounted) Navigator.of(ctx).pop();
                        messenger.showSnackBar(
                          const SnackBar(content: Text('Đã xác nhận giao hàng')),
                        );
                        _fetch();
                      } on DioException catch (e) {
                        final msg = e.response?.data is Map
                            ? '${(e.response!.data as Map)['message']}'
                            : e.message;
                        if (!context.mounted) return;
                        messenger.showSnackBar(
                          SnackBar(content: Text(msg ?? 'Lỗi'), backgroundColor: Colors.redAccent),
                        );
                      }
                    },
                    icon: const Icon(Icons.check_circle_outline),
                    label: const Text('Xác nhận đã giao tới người mua'),
                  ),
                ),
              ],
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error_outline, size: 48, color: Colors.red.shade300),
              const SizedBox(height: 12),
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              FilledButton(onPressed: _fetch, child: const Text('Thử lại')),
            ],
          ),
        ),
      );
    }
    if (_orders.isEmpty) {
      return RefreshIndicator(
        onRefresh: _fetch,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          children: const [
            SizedBox(height: 120),
            Center(child: Text('Chưa có đơn hàng được gán cho bạn.')),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _fetch,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
        itemCount: _orders.length,
        separatorBuilder: (_, __) => const SizedBox(height: 8),
        itemBuilder: (context, i) {
          final o = _orders[i];
          return Card(
            elevation: 0,
            color: Colors.grey.shade50,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
            child: ListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              leading: CircleAvatar(
                backgroundColor: Colors.orange.shade100,
                child: Icon(Icons.local_shipping_rounded, color: Colors.orange.shade800),
              ),
              title: Text(
                o.orderCode,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              subtitle: Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(_statusLabel(o.status)),
                    const SizedBox(height: 4),
                    Text(
                      '${o.lines.length} dòng · ${o.orderType}',
                      style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                    ),
                  ],
                ),
              ),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _openDetail(o),
            ),
          );
        },
      ),
    );
  }
}

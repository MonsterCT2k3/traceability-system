import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';

import '../../../../injection_container.dart';
import '../../data/trade_order_models.dart';
import '../../domain/usecases/get_order_details.dart';
import '../../domain/usecases/resolve_user_label.dart';
import '../../domain/usecases/resolve_batch_name.dart';
import '../../domain/usecases/resolve_product_name.dart';
import '../bloc/transporter_orders_bloc.dart';
import '../bloc/transporter_orders_event.dart';
import '../bloc/transporter_orders_state.dart';

/// Danh sách đơn được gán cho VC: nhận hàng tại NCC/NSX → xác nhận giao tới người mua.
class TransporterOrdersTab extends StatelessWidget {
  const TransporterOrdersTab({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => sl<TransporterOrdersBloc>()..add(FetchTransporterOrdersEvent()),
      child: const TransporterOrdersView(),
    );
  }
}

class TransporterOrdersView extends StatefulWidget {
  const TransporterOrdersView({super.key});

  @override
  State<TransporterOrdersView> createState() => _TransporterOrdersViewState();
}

class _TransporterOrdersViewState extends State<TransporterOrdersView> {
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >= _scrollController.position.maxScrollExtent - 200) {
      context.read<TransporterOrdersBloc>().add(LoadMoreOrdersEvent());
    }
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
        return 'Chờ nhận hàng';
      case 'PICKED_UP_FROM_SELLER':
        return 'Đang giao hàng';
      case 'DELIVERED':
        return 'Đã giao';
      default:
        return s;
    }
  }

  Color _statusColor(String s) {
    switch (s) {
      case 'PENDING':
        return Colors.orange;
      case 'ACCEPTED':
        return Colors.blue;
      case 'REJECTED':
      case 'CANCELLED':
        return Colors.red;
      case 'ASSIGNED_TO_CARRIER':
        return Colors.purple;
      case 'PICKED_UP_FROM_SELLER':
        return Colors.teal;
      case 'DELIVERED':
        return Colors.green;
      default:
        return Colors.grey;
    }
  }

  IconData _statusIcon(String s) {
    switch (s) {
      case 'PENDING':
        return Icons.hourglass_empty;
      case 'ACCEPTED':
        return Icons.check_circle_outline;
      case 'REJECTED':
      case 'CANCELLED':
        return Icons.cancel_outlined;
      case 'ASSIGNED_TO_CARRIER':
        return Icons.storefront_outlined;
      case 'PICKED_UP_FROM_SELLER':
        return Icons.local_shipping_outlined;
      case 'DELIVERED':
        return Icons.done_all;
      default:
        return Icons.info_outline;
    }
  }

  Future<void> _openDetail(TradeOrderDto order) async {
    final messenger = ScaffoldMessenger.of(context);
    final getDetails = sl<GetOrderDetailsUseCase>();
    final resolveLabel = sl<ResolveUserLabelUseCase>();
    final resolveBatch = sl<ResolveBatchNameUseCase>();
    final resolveProduct = sl<ResolveProductNameUseCase>();

    // Hiển thị loading trong khi fetch
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    TradeOrderDto detail = order;
    try {
      detail = await getDetails(order.id);
    } catch (_) {}

    String buyerLine = detail.buyerId;
    String sellerLine = detail.sellerId;
    try {
      final resolvedUsers = await Future.wait([
        resolveLabel(detail.buyerId),
        resolveLabel(detail.sellerId),
      ]);
      buyerLine = resolvedUsers[0];
      sellerLine = resolvedUsers[1];
    } catch (_) {}

    List<String> resolvedLines = [];
    for (var l in detail.lines) {
      String lineName = '—';
      if (l.targetRawBatchId != null) {
        try {
          lineName = await resolveBatch(l.targetRawBatchId!);
        } catch (_) {}
      } else if (l.productId != null) {
        try {
          lineName = await resolveProduct(l.productId!);
        } catch (_) {}
      }

      String qty = l.quantityRequested != null
          ? '${l.quantityRequested} ${l.unit ?? ''}'
          : (l.quantityCartons != null ? '${l.quantityCartons} thùng' : '');

      resolvedLines.add('#${l.lineIndex ?? 0} · $lineName · $qty');
    }

    if (!mounted) return;
    
    // ignore: use_build_context_synchronously
    Navigator.of(context).pop(); // Tắt loading dialog

    // ignore: use_build_context_synchronously
    final bloc = context.read<TransporterOrdersBloc>();

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
                ...resolvedLines.map(
                  (txt) => Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      txt,
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
                      bloc.add(ConfirmPickedUpEvent(detail));
                      if (ctx.mounted) Navigator.of(ctx).pop();
                      messenger.showSnackBar(
                        const SnackBar(content: Text('Đã gửi yêu cầu xác nhận')),
                      );
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
                      bloc.add(ConfirmDeliveredEvent(detail));
                      if (ctx.mounted) Navigator.of(ctx).pop();
                      messenger.showSnackBar(
                        const SnackBar(content: Text('Đã gửi yêu cầu xác nhận')),
                      );
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
    return BlocConsumer<TransporterOrdersBloc, TransporterOrdersState>(
      listener: (context, state) {
        if (state is TransporterOrdersError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.message), backgroundColor: Colors.redAccent),
          );
        }
      },
      builder: (context, state) {
        if (state is TransporterOrdersLoading || state is TransporterOrdersInitial) {
          return const Center(child: CircularProgressIndicator());
        }

        if (state is TransporterOrdersError && state is! TransporterOrdersLoaded) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.error_outline, size: 48, color: Colors.red.shade300),
                  const SizedBox(height: 12),
                  Text(state.message, textAlign: TextAlign.center),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: () => context.read<TransporterOrdersBloc>().add(FetchTransporterOrdersEvent()),
                    child: const Text('Thử lại'),
                  ),
                ],
              ),
            ),
          );
        }

        if (state is TransporterOrdersLoaded) {
          if (state.allOrders.isEmpty) {
            return RefreshIndicator(
              onRefresh: () async => context.read<TransporterOrdersBloc>().add(FetchTransporterOrdersEvent()),
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
            onRefresh: () async => context.read<TransporterOrdersBloc>().add(FetchTransporterOrdersEvent()),
            child: ListView.separated(
              controller: _scrollController,
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
              itemCount: state.displayOrders.length + (state.isLoadingMore ? 1 : 0),
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, i) {
                if (i == state.displayOrders.length) {
                  return const Padding(
                    padding: EdgeInsets.all(16.0),
                    child: Center(child: CircularProgressIndicator()),
                  );
                }
                final o = state.displayOrders[i];
                return Card(
                  elevation: 0,
                  color: Colors.grey.shade50,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    leading: CircleAvatar(
                      backgroundColor: _statusColor(o.status).withOpacity(0.15),
                      child: Icon(_statusIcon(o.status), color: _statusColor(o.status)),
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
                          Text(
                            _statusLabel(o.status),
                            style: TextStyle(
                              color: _statusColor(o.status),
                              fontWeight: FontWeight.w600,
                            ),
                          ),
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

        return const SizedBox();
      },
    );
  }
}

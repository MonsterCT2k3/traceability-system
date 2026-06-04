import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/network/api_client.dart';
import '../../data/models/trace_model.dart';
import '../../domain/entities/trace_entity.dart';

const _green = Color(0xFF13795B);
const _ink = Color(0xFF17352D);
const _surface = Color(0xFFF4F8F5);

class DirectInputsSection extends StatelessWidget {
  final DirectTraceEntity trace;

  const DirectInputsSection({super.key, required this.trace});

  @override
  Widget build(BuildContext context) =>
      _DirectTraceContent(initialTrace: trace);
}

class _DirectTracePage extends StatefulWidget {
  final String palletId;

  const _DirectTracePage({required this.palletId});

  @override
  State<_DirectTracePage> createState() => _DirectTracePageState();
}

class _DirectTracePageState extends State<_DirectTracePage> {
  DirectTraceEntity? trace;
  Object? loadError;
  bool verificationFailed = false;

  @override
  void initState() {
    super.initState();
    _loadThenVerify();
  }

  Future<void> _loadThenVerify() async {
    try {
      final loaded = await _loadDirectTrace(widget.palletId);
      if (!mounted) return;
      setState(() => trace = loaded);
      try {
        final verified = await _verifyDirectTrace(widget.palletId);
        if (mounted) setState(() => trace = verified);
      } catch (_) {
        if (mounted) setState(() => verificationFailed = true);
      }
    } catch (e) {
      if (mounted) setState(() => loadError = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(title: const Text('Nguồn gốc nguyên liệu')),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (loadError != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text('Không tải được nguồn gốc: $loadError'),
        ),
      );
    }
    if (trace == null) return const Center(child: CircularProgressIndicator());

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          if (verificationFailed) ...[
            const _VerificationFailedBanner(),
            const SizedBox(height: 12),
          ],
          _DirectTraceContent(initialTrace: trace!, showCurrentNode: true),
        ],
      ),
    );
  }
}

class _VerificationFailedBanner extends StatelessWidget {
  const _VerificationFailedBanner();

  @override
  Widget build(BuildContext context) {
    return _StatusBanner(
      color: Colors.red.shade700,
      icon: Icon(Icons.error_outline, color: Colors.red.shade700),
      text: 'Không thể tự xác minh. Bạn có thể xác minh lại bên dưới.',
    );
  }
}

class _StatusBanner extends StatelessWidget {
  final Color color;
  final Widget icon;
  final String text;

  const _StatusBanner({
    required this.color,
    required this.icon,
    required this.text,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: color.withOpacity(.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withOpacity(.25)),
      ),
      child: Row(
        children: [
          icon,
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: TextStyle(color: color, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

class _DirectTraceContent extends StatefulWidget {
  final DirectTraceEntity initialTrace;
  final bool showCurrentNode;

  const _DirectTraceContent(
      {required this.initialTrace, this.showCurrentNode = false});

  @override
  State<_DirectTraceContent> createState() => _DirectTraceContentState();
}

class _DirectTraceContentState extends State<_DirectTraceContent> {
  late DirectTraceEntity trace;

  @override
  void initState() {
    super.initState();
    trace = widget.initialTrace;
  }

  @override
  void didUpdateWidget(covariant _DirectTraceContent oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.initialTrace != widget.initialTrace) {
      trace = widget.initialTrace;
    }
  }

  @override
  Widget build(BuildContext context) {
    final summary = trace.verificationSummary;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (widget.showCurrentNode) ...[
          _CurrentNodeHeader(node: trace.currentNode),
          const SizedBox(height: 16),
        ],
        Container(
          width: double.infinity,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: const Color(0xFFDCE9E2)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
                child: Row(
                  children: [
                    const Icon(Icons.account_tree_outlined, color: _green),
                    const SizedBox(width: 10),
                    const Expanded(
                      child: Text('Nguyên liệu đầu vào',
                          style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.w700,
                              color: _ink)),
                    ),
                    _CountBadge(count: trace.directInputs.length),
                  ],
                ),
              ),
              Divider(height: 1, color: Colors.grey.shade200),
              if (trace.directInputs.isEmpty)
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Text(
                      'Đây là nguyên liệu gốc hoặc lô chưa khai báo đầu vào.'),
                )
              else
                ...trace.directInputs.asMap().entries.map(
                      (entry) => _InputRow(
                        node: entry.value,
                        onTap: () => _showNodeDetails(context, entry.value),
                      ),
                    ),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius:
                      BorderRadius.vertical(bottom: Radius.circular(7)),
                ),
                child: _VerificationConclusion(summary: summary),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _VerificationConclusion extends StatelessWidget {
  final DirectVerificationSummaryEntity? summary;

  const _VerificationConclusion({required this.summary});

  @override
  Widget build(BuildContext context) {
    final status = summary?.overallStatus ?? 'NOT_VERIFIED';
    final verified = status == 'VERIFIED';
    final mismatch = status == 'MISMATCH';
    final currentNodeMismatch = summary?.currentNodeStatus == 'MISMATCH';
    final relationMismatch = summary?.inputRelationStatus == 'MISMATCH';
    final mismatchedInputCount =
        (summary?.totalInputCount ?? 0) - (summary?.verifiedInputCount ?? 0);
    final color = verified
        ? _green
        : mismatch
            ? Colors.red.shade700
            : Colors.orange.shade800;
    final backgroundColor = verified
        ? const Color(0xFFE4F4EC)
        : mismatch
            ? const Color(0xFFFDE8E8)
            : const Color(0xFFFFF3D9);
    final title = verified
        ? 'Lô và nguyên liệu đều hợp lệ'
        : currentNodeMismatch
            ? 'Thông tin lô đang xem không khớp'
            : relationMismatch
                ? 'Quan hệ nguyên liệu đầu vào không khớp'
                : mismatch
                    ? 'Có nguyên liệu không khớp blockchain'
                    : 'Đang xác minh blockchain';
    final detail = verified
        ? 'Lô và ${summary!.verifiedInputCount}/${summary!.totalInputCount} đầu vào trực tiếp đều đã được xác minh'
        : currentNodeMismatch
            ? 'Dữ liệu của lô đã bị thay đổi. ${summary?.verifiedInputCount ?? 0}/${summary?.totalInputCount ?? 0} nguyên liệu đầu vào trực tiếp vẫn hợp lệ.'
            : relationMismatch
                ? 'Danh sách nguyên liệu tạo nên lô không khớp dữ liệu đã ghi trên blockchain.'
                : mismatch
                    ? '$mismatchedInputCount/${summary?.totalInputCount ?? 0} nguyên liệu đầu vào không khớp. Các nguyên liệu sai lệch được đánh dấu màu đỏ.'
                    : 'Kết quả sẽ tự động cập nhật khi xác minh hoàn tất';

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withOpacity(.32), width: 1.2),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 54,
            height: 54,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
              boxShadow: [
                BoxShadow(
                  color: color.withOpacity(.2),
                  blurRadius: 8,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: status == 'NOT_VERIFIED'
                ? const Padding(
                    padding: EdgeInsets.all(16),
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: Colors.white,
                    ),
                  )
                : Icon(
                    verified ? Icons.verified_rounded : Icons.gpp_bad_rounded,
                    size: 32,
                    color: Colors.white,
                  ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: color.withOpacity(.12),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    'BLOCKCHAIN',
                    style: TextStyle(
                      color: color,
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(height: 7),
                Text(
                  title,
                  style: TextStyle(
                    color: color,
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  detail,
                  style: TextStyle(
                    color: _ink.withOpacity(.75),
                    fontSize: 13,
                    height: 1.35,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CurrentNodeHeader extends StatelessWidget {
  final TraceNodeEntity node;

  const _CurrentNodeHeader({required this.node});

  @override
  Widget build(BuildContext context) {
    final mismatch = node.verificationStatus == 'MISMATCH';
    final verified = node.verificationStatus == 'VERIFIED';
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration:
          BoxDecoration(color: _ink, borderRadius: BorderRadius.circular(8)),
      child: Row(
        children: [
          const CircleAvatar(
            backgroundColor: Colors.white24,
            child: Icon(Icons.inventory_2_outlined, color: Colors.white),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(node.name,
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 17,
                        fontWeight: FontWeight.w700)),
                const SizedBox(height: 2),
                Text(node.code, style: const TextStyle(color: Colors.white70)),
                if (node.verificationStatus != 'NOT_VERIFIED') ...[
                  const SizedBox(height: 9),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: mismatch
                          ? Colors.red.shade600
                          : Colors.white.withOpacity(.14),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          verified
                              ? Icons.verified_rounded
                              : Icons.warning_amber_rounded,
                          size: 14,
                          color: Colors.white,
                        ),
                        const SizedBox(width: 5),
                        Text(
                          verified
                              ? 'Lô đã xác minh'
                              : 'Thông tin lô không khớp',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _InputRow extends StatelessWidget {
  final TraceNodeEntity node;
  final VoidCallback onTap;

  const _InputRow({required this.node, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final isPallet = node.nodeType == 'PALLET';
    final mismatch = node.verificationStatus == 'MISMATCH';
    final verified = node.verificationStatus == 'VERIFIED';
    final statusColor = mismatch
        ? Colors.red.shade700
        : verified
            ? _green
            : Colors.orange.shade800;
    final backgroundColor = mismatch
        ? const Color(0xFFFFEEEE)
        : verified
            ? Colors.white
            : const Color(0xFFFFFAF0);

    return Container(
      margin: const EdgeInsets.fromLTRB(10, 8, 10, 0),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: mismatch
              ? Colors.red.shade300
              : verified
                  ? Colors.transparent
                  : Colors.orange.shade200,
          width: mismatch ? 1.4 : 1,
        ),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 12, 10, 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.all(2),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: mismatch ? statusColor : Colors.transparent,
                    width: 2,
                  ),
                ),
                child: _ActorAvatar(node: node, radius: 23),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      node.name,
                      style: TextStyle(
                        color: mismatch ? Colors.red.shade900 : _ink,
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      node.actorName?.isNotEmpty == true
                          ? node.actorName!
                          : 'Chưa có tên đơn vị',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(color: Colors.grey.shade600),
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            isPallet ? 'Lô từ nhà sản xuất' : 'Nguyên liệu gốc',
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: _green,
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        _InputVerificationBadge(
                          status: node.verificationStatus,
                          color: statusColor,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 6),
              Icon(
                mismatch
                    ? Icons.warning_amber_rounded
                    : Icons.chevron_right_rounded,
                color: mismatch ? statusColor : Colors.black38,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _InputVerificationBadge extends StatelessWidget {
  final String status;
  final Color color;

  const _InputVerificationBadge({required this.status, required this.color});

  @override
  Widget build(BuildContext context) {
    final label = status == 'VERIFIED'
        ? 'Đã xác minh'
        : status == 'MISMATCH'
            ? 'Không khớp'
            : 'Đang xác minh';
    final icon = status == 'VERIFIED'
        ? Icons.verified_rounded
        : status == 'MISMATCH'
            ? Icons.error_outline_rounded
            : Icons.sync_rounded;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        color: color.withOpacity(.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: color),
          const SizedBox(width: 3),
          Text(
            label,
            style: TextStyle(
              color: color,
              fontSize: 10,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _ActorAvatar extends StatelessWidget {
  final TraceNodeEntity node;
  final double radius;

  const _ActorAvatar({required this.node, required this.radius});

  @override
  Widget build(BuildContext context) {
    final url = node.actorAvatarUrl;
    final fallbackName = node.name.trim().isEmpty ? '?' : node.name.trim();
    final initial = (node.actorName?.trim().isNotEmpty == true
            ? node.actorName!.trim()[0]
            : fallbackName[0])
        .toUpperCase();
    return CircleAvatar(
      radius: radius,
      backgroundColor: const Color(0xFFDCEFE5),
      foregroundImage: url?.isNotEmpty == true ? NetworkImage(url!) : null,
      child: Text(initial,
          style: const TextStyle(color: _green, fontWeight: FontWeight.w700)),
    );
  }
}

class _CountBadge extends StatelessWidget {
  final int count;

  const _CountBadge({required this.count});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(
          color: const Color(0xFFDCEFE5),
          borderRadius: BorderRadius.circular(14)),
      child: Text('$count đầu vào',
          style: const TextStyle(
              color: _green, fontSize: 12, fontWeight: FontWeight.w700)),
    );
  }
}

void _showNodeDetails(BuildContext context, TraceNodeEntity node) {
  showModalBottomSheet(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
    builder: (context) => SafeArea(
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * .9,
        ),
        child: _NodeDetails(node: node),
      ),
    ),
  );
}

class _NodeDetails extends StatelessWidget {
  final TraceNodeEntity node;

  const _NodeDetails({required this.node});

  @override
  Widget build(BuildContext context) {
    final isPallet = node.nodeType == 'PALLET';
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 10, 20, 28),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
                width: 42,
                height: 4,
                decoration: BoxDecoration(
                    color: Colors.black12,
                    borderRadius: BorderRadius.circular(4))),
          ),
          const SizedBox(height: 22),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _ActorAvatar(node: node, radius: 34),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(node.name,
                        style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w800,
                            color: _ink)),
                    const SizedBox(height: 5),
                    Text(node.code,
                        style: TextStyle(color: Colors.grey.shade600)),
                    const SizedBox(height: 8),
                    Text(
                        isPallet
                            ? 'Sản xuất bởi ${node.actorName ?? 'Chưa xác định'}'
                            : 'Cung cấp bởi ${node.actorName ?? 'Chưa xác định'}',
                        style: const TextStyle(
                            color: _green, fontWeight: FontWeight.w600)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 22),
          _VerificationPanel(
              status: node.verificationStatus,
              batchId: node.blockchainBatchIdHex),
          const SizedBox(height: 20),
          const Text('Thông tin lô',
              style: TextStyle(
                  fontSize: 16, fontWeight: FontWeight.w700, color: _ink)),
          const SizedBox(height: 8),
          _DetailRow(
              icon: Icons.calendar_today_outlined,
              label: isPallet ? 'Ngày sản xuất' : 'Ngày thu hoạch',
              value: _formatDate(node.occurredAt)),
          _DetailRow(
              icon: Icons.scale_outlined,
              label: 'Số lượng',
              value: _join(node.quantity, node.unit)),
          _DetailRow(
              icon: Icons.location_on_outlined,
              label: 'Địa điểm',
              value: node.location),
          if (isPallet)
            _DetailRow(
                icon: Icons.event_busy_outlined,
                label: 'Hạn sử dụng',
                value: _formatDate(node.expiryAt)),
          if (isPallet && node.hasInputs) ...[
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: () {
                  Navigator.pop(context);
                  Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (_) => _DirectTracePage(palletId: node.id)));
                },
                icon: const Icon(Icons.account_tree_outlined),
                label: const Text('Xem nguyên liệu tạo nên lô này'),
                style: FilledButton.styleFrom(
                    backgroundColor: _green,
                    padding: const EdgeInsets.symmetric(vertical: 14)),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _VerificationPanel extends StatelessWidget {
  final String status;
  final String? batchId;

  const _VerificationPanel({required this.status, this.batchId});

  @override
  Widget build(BuildContext context) {
    final verified = status == 'VERIFIED';
    final mismatch = status == 'MISMATCH';
    final color = verified
        ? _green
        : (mismatch ? Colors.red.shade700 : Colors.orange.shade800);
    final label = verified
        ? 'Đã xác minh trên blockchain'
        : (mismatch
            ? 'Dữ liệu không khớp blockchain'
            : 'Chưa xác minh blockchain');
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
          color: color.withOpacity(.08),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: color.withOpacity(.25))),
      child: Row(
        children: [
          Icon(verified ? Icons.verified_rounded : Icons.shield_outlined,
              color: color),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label,
                    style:
                        TextStyle(color: color, fontWeight: FontWeight.w700)),
                if (batchId?.isNotEmpty == true) ...[
                  const SizedBox(height: 3),
                  Text(_shortHash(batchId!),
                      style:
                          TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String? value;

  const _DetailRow({required this.icon, required this.label, this.value});

  @override
  Widget build(BuildContext context) {
    if (value == null || value!.trim().isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 9),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 19, color: _green),
          const SizedBox(width: 11),
          SizedBox(
              width: 116,
              child:
                  Text(label, style: TextStyle(color: Colors.grey.shade600))),
          Expanded(
              child: Text(value!,
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, color: _ink))),
        ],
      ),
    );
  }
}

String? _join(String? first, String? second) {
  final values = [first, second]
      .where((value) => value?.trim().isNotEmpty == true)
      .toList();
  return values.isEmpty ? null : values.join(' ');
}

String? _formatDate(String? value) {
  if (value == null || value.trim().isEmpty) return null;
  try {
    final date = DateTime.parse(value).toLocal();
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
  } catch (_) {
    return value;
  }
}

String _shortHash(String value) => value.length > 22
    ? '${value.substring(0, 12)}...${value.substring(value.length - 8)}'
    : value;

Future<DirectTraceEntity> _verifyDirectTrace(String palletId) async {
  final response = await GetIt.I<ApiClient>()
      .get('/product/api/v1/pallets/$palletId/verify-direct');
  return directTraceFromJson(
      Map<String, dynamic>.from(response.data['result'] as Map));
}

Future<DirectTraceEntity> _loadDirectTrace(String palletId) async {
  final response = await GetIt.I<ApiClient>()
      .get('/product/api/v1/pallets/$palletId/trace-direct');
  return directTraceFromJson(
      Map<String, dynamic>.from(response.data['result'] as Map));
}

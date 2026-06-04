import 'package:flutter_test/flutter_test.dart';
import 'package:frontend_mobile/features/main/data/models/trace_model.dart';

void main() {
  test('parses direct trace response', () {
    final trace = directTraceFromJson({
      'currentNode': {
        'id': 'output',
        'nodeType': 'PALLET',
        'code': 'PALLET-1',
        'name': 'Banh sua',
        'hasInputs': true,
        'verificationStatus': 'NOT_VERIFIED',
      },
      'directInputs': [
        {
          'id': 'input',
          'nodeType': 'PALLET',
          'code': 'PALLET-0',
          'name': 'Sua dac',
          'actorName': 'Nha may sua',
          'actorAvatarUrl': 'https://example.com/avatar.png',
          'occurredAt': '2026-06-04',
          'quantity': '500',
          'unit': 'kg',
          'processingMethod': 'Co dac',
          'blockchainBatchIdHex': '0x1234',
          'hasInputs': true,
          'verificationStatus': 'VERIFIED',
        }
      ],
      'verificationScope': 'CURRENT_AND_DIRECT_INPUTS',
    });

    expect(trace.currentNode.id, 'output');
    expect(trace.directInputs.single.name, 'Sua dac');
    expect(trace.directInputs.single.hasInputs, isTrue);
    expect(trace.directInputs.single.actorName, 'Nha may sua');
    expect(trace.directInputs.single.quantity, '500');
    expect(trace.directInputs.single.processingMethod, 'Co dac');
    expect(trace.directInputs.single.blockchainBatchIdHex, '0x1234');
  });
}

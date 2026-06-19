
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * Records raw/transformed batch hashes and ownership audit events.
 * Only the system wallet can write traceability data.
 */
contract Traceability {

    error NotSystemWallet();
    error SystemWalletIsZero();
    error BatchAlreadyRecorded();
    error TransformedBatchAlreadyRecorded();

    struct BatchInfo {
        bytes32 dataHash;
        uint256 timestamp;
    }

    struct TransformedBatchInfo {
        bytes32 dataHash;
        bytes32 parentRoot;
        uint256 timestamp;
    }

    address public immutable systemWallet;

    mapping(bytes32 => BatchInfo) private batches;
    mapping(bytes32 => TransformedBatchInfo) private transformedBatches;

    event BatchRecorded(
        bytes32 indexed batchId,
        bytes32 dataHash,
        address indexed actor,
        uint256 timestamp
    );

    event TransformedBatchRecorded(
        bytes32 indexed batchId,
        bytes32 dataHash,
        bytes32 parentRoot,
        address indexed actor,
        uint256 timestamp
    );

    event OwnershipChanged(
        bytes32 indexed batchId,
        string fromUserId,
        string toUserId,
        uint256 timestamp
    );

    modifier onlySystem() {
        if (msg.sender != systemWallet) revert NotSystemWallet();
        _;
    }

    constructor(address _systemWallet) {
        if (_systemWallet == address(0)) revert SystemWalletIsZero();
        systemWallet = _systemWallet;
    }

    function recordBatch(bytes32 _batchId, bytes32 _dataHash) external onlySystem {
        if (batches[_batchId].timestamp != 0) revert BatchAlreadyRecorded();
        batches[_batchId] = BatchInfo({
            dataHash: _dataHash,
            timestamp: block.timestamp
        });
        emit BatchRecorded(_batchId, _dataHash, msg.sender, block.timestamp);
    }

    function recordTransformedBatch(
        bytes32 _batchId,
        bytes32 _dataHash,
        bytes32[] calldata _parentHashes
    ) external onlySystem {
        if (transformedBatches[_batchId].timestamp != 0) revert TransformedBatchAlreadyRecorded();
        bytes32 parentRoot = keccak256(abi.encodePacked(_parentHashes));
        transformedBatches[_batchId] = TransformedBatchInfo({
            dataHash: _dataHash,
            parentRoot: parentRoot,
            timestamp: block.timestamp
        });
        emit TransformedBatchRecorded(_batchId, _dataHash, parentRoot, msg.sender, block.timestamp);
    }

    function logOwnershipChange(
        bytes32 _batchId,
        string calldata _fromUserId,
        string calldata _toUserId
    ) external onlySystem {
        emit OwnershipChanged(_batchId, _fromUserId, _toUserId, block.timestamp);
    }

    function getBatchRecord(bytes32 _batchId)
        external view
        returns (bytes32 dataHash, address actor, uint256 timestamp)
    {
        BatchInfo memory b = batches[_batchId];
        address actor = b.timestamp == 0 ? address(0) : systemWallet;
        return (b.dataHash, actor, b.timestamp);
    }

    function getTransformedBatchRecord(bytes32 _batchId)
        external view
        returns (bytes32 dataHash, bytes32 parentRoot, address actor, uint256 timestamp)
    {
        TransformedBatchInfo memory t = transformedBatches[_batchId];
        address actor = t.timestamp == 0 ? address(0) : systemWallet;
        return (t.dataHash, t.parentRoot, actor, t.timestamp);
    }

    function hasBatch(bytes32 _batchId) external view returns (bool) {
        return batches[_batchId].timestamp != 0;
    }

    function hasTransformedBatch(bytes32 _batchId) external view returns (bool) {
        return transformedBatches[_batchId].timestamp != 0;
    }
}

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * Traceability – ghi Batch/Pallet + audit chuyển quyền (userId) qua event.
 * Chỉ systemWallet được ghi (recordBatch, recordTransformedBatch, logOwnershipChange).
 */
contract Traceability {

    struct BatchInfo {
        bytes32 dataHash;
        address actor;
        uint256 timestamp;
    }

    struct TransformedBatchInfo {
        bytes32 dataHash;
        bytes32 parentRoot;
        address actor;
        uint256 timestamp;
    }

    address public systemWallet;

    mapping(bytes32 => BatchInfo) public batches;
    mapping(bytes32 => TransformedBatchInfo) public transformedBatches;

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
        require(msg.sender == systemWallet, "Not system wallet");
        _;
    }

    constructor(address _systemWallet) {
        require(_systemWallet != address(0), "System wallet is zero");
        systemWallet = _systemWallet;
    }

    function recordBatch(bytes32 _batchId, bytes32 _dataHash) external onlySystem {
        require(batches[_batchId].timestamp == 0, "Batch already recorded");
        batches[_batchId] = BatchInfo({
            dataHash: _dataHash,
            actor: msg.sender,
            timestamp: block.timestamp
        });
        emit BatchRecorded(_batchId, _dataHash, msg.sender, block.timestamp);
    }

    function recordTransformedBatch(
        bytes32 _batchId,
        bytes32 _dataHash,
        bytes32[] calldata _parentHashes
    ) external onlySystem {
        require(transformedBatches[_batchId].timestamp == 0, "Transformed batch already recorded");
        bytes32 parentRoot = keccak256(abi.encodePacked(_parentHashes));
        transformedBatches[_batchId] = TransformedBatchInfo({
            dataHash: _dataHash,
            parentRoot: parentRoot,
            actor: msg.sender,
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
        return (b.dataHash, b.actor, b.timestamp);
    }

    function getTransformedBatchRecord(bytes32 _batchId)
        external view
        returns (bytes32 dataHash, bytes32 parentRoot, address actor, uint256 timestamp)
    {
        TransformedBatchInfo memory t = transformedBatches[_batchId];
        return (t.dataHash, t.parentRoot, t.actor, t.timestamp);
    }

    function hasBatch(bytes32 _batchId) external view returns (bool) {
        return batches[_batchId].timestamp != 0;
    }

    function hasTransformedBatch(bytes32 _batchId) external view returns (bool) {
        return transformedBatches[_batchId].timestamp != 0;
    }
}
package vn.edu.kma.blockchain_service.contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.FunctionEncoder;

/**
 * Java wrapper cho contract Traceability.sol (chỉ mức Batch/Pallet).
 * BINARY: dán bytecode sau khi compile Traceability.sol (solc 0.8.19).
 */
@SuppressWarnings("rawtypes")
public class Traceability extends Contract {

    /**
     * Dán bytecode từ file .bin (bỏ tiền tố 0x) sau khi compile Traceability.sol
     */
    public static final String BINARY = "60806040523480156200001157600080fd5b506040516200127e3803806200127e83398181016040528101906200003791906200015a565b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603620000a9576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401620000a090620001ed565b60405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506200020f565b600080fd5b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006200012282620000f5565b9050919050565b620001348162000115565b81146200014057600080fd5b50565b600081519050620001548162000129565b92915050565b600060208284031215620001735762000172620000f0565b5b6000620001838482850162000143565b91505092915050565b600082825260208201905092915050565b7f53797374656d2077616c6c6574206973207a65726f0000000000000000000000600082015250565b6000620001d56015836200018c565b9150620001e2826200019d565b602082019050919050565b600060208201905081810360008301526200020881620001c6565b9050919050565b61105f806200021f6000396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c8063ad7d3a7211610066578063ad7d3a721461016d578063b18dce581461018b578063b9a1658a146101a7578063c81e25ab146101da578063e2e18b161461020c5761009e565b8063173fd027146100a35780631b9ce039146100bf5780633ee02e7b146100f157806342752fa614610121578063a0a0df3d1461013d575b600080fd5b6100bd60048036038101906100b891906109c3565b61023f565b005b6100d960048036038101906100d49190610a03565b61041d565b6040516100e893929190610a99565b60405180910390f35b61010b60048036038101906101069190610a03565b6104ca565b6040516101189190610aeb565b60405180910390f35b61013b60048036038101906101369190610b6b565b6104ed565b005b61015760048036038101906101529190610a03565b6105c2565b6040516101649190610aeb565b60405180910390f35b6101756105e5565b6040516101829190610c00565b60405180910390f35b6101a560048036038101906101a09190610c71565b610609565b005b6101c160048036038101906101bc9190610a03565b610829565b6040516101d19493929190610ce5565b60405180910390f35b6101f460048036038101906101ef9190610a03565b610879565b60405161020393929190610a99565b60405180910390f35b61022660048036038101906102219190610a03565b6108c3565b6040516102369493929190610ce5565b60405180910390f35b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16146102cd576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016102c490610d87565b60405180910390fd5b6000600160008481526020019081526020016000206002015414610326576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161031d90610df3565b60405180910390fd5b60405180606001604052808281526020013373ffffffffffffffffffffffffffffffffffffffff16815260200142815250600160008481526020019081526020016000206000820151816000015560208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550604082015181600201559050503373ffffffffffffffffffffffffffffffffffffffff16827f9625ae2ab8825c8b6070e01fc994e3ff9b0acde223ee402568856d02170b1e628342604051610411929190610e13565b60405180910390a35050565b60008060008060016000868152602001908152602001600020604051806060016040529081600082015481526020016001820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016002820154815250509050806000015181602001518260400151935093509350509193909250565b600080600260008481526020019081526020016000206003015414159050919050565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461057b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161057290610d87565b60405180910390fd5b847fd4feedd7eb1e263f0f1c630099fe7e3a4bd82b2ed1359ab9b75e0ca3af330af685858585426040516105b3959493929190610e89565b60405180910390a25050505050565b600080600160008481526020019081526020016000206002015414159050919050565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610697576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161068e90610d87565b60405180910390fd5b60006002600086815260200190815260200160002060030154146106f0576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016106e790610f44565b60405180910390fd5b60008282604051602001610705929190610fd9565b60405160208183030381529060405280519060200120905060405180608001604052808581526020018281526020013373ffffffffffffffffffffffffffffffffffffffff1681526020014281525060026000878152602001908152602001600020600082015181600001556020820151816001015560408201518160020160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550606082015181600301559050503373ffffffffffffffffffffffffffffffffffffffff16857f232e79352ac0dba2afb4cb6f5a053fa184ed21d6875c42d1a696fb4f9641a70686844260405161081a93929190610ff2565b60405180910390a35050505050565b60026020528060005260406000206000915090508060000154908060010154908060020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16908060030154905084565b60016020528060005260406000206000915090508060000154908060010160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16908060020154905083565b60008060008060006002600087815260200190815260200160002060405180608001604052908160008201548152602001600182015481526020016002820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001600382015481525050905080600001518160200151826040015183606001519450945094509450509193509193565b600080fd5b600080fd5b6000819050919050565b6109a08161098d565b81146109ab57600080fd5b50565b6000813590506109bd81610997565b92915050565b600080604083850312156109da576109d9610983565b5b60006109e8858286016109ae565b92505060206109f9858286016109ae565b9150509250929050565b600060208284031215610a1957610a18610983565b5b6000610a27848285016109ae565b91505092915050565b610a398161098d565b82525050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b6000610a6a82610a3f565b9050919050565b610a7a81610a5f565b82525050565b6000819050919050565b610a9381610a80565b82525050565b6000606082019050610aae6000830186610a30565b610abb6020830185610a71565b610ac86040830184610a8a565b949350505050565b60008115159050919050565b610ae581610ad0565b82525050565b6000602082019050610b006000830184610adc565b92915050565b600080fd5b600080fd5b600080fd5b60008083601f840112610b2b57610b2a610b06565b5b8235905067ffffffffffffffff811115610b4857610b47610b0b565b5b602083019150836001820283011115610b6457610b63610b10565b5b9250929050565b600080600080600060608688031215610b8757610b86610983565b5b6000610b95888289016109ae565b955050602086013567ffffffffffffffff811115610bb657610bb5610988565b5b610bc288828901610b15565b9450945050604086013567ffffffffffffffff811115610be557610be4610988565b5b610bf188828901610b15565b92509250509295509295909350565b6000602082019050610c156000830184610a71565b92915050565b60008083601f840112610c3157610c30610b06565b5b8235905067ffffffffffffffff811115610c4e57610c4d610b0b565b5b602083019150836020820283011115610c6a57610c69610b10565b5b9250929050565b60008060008060608587031215610c8b57610c8a610983565b5b6000610c99878288016109ae565b9450506020610caa878288016109ae565b935050604085013567ffffffffffffffff811115610ccb57610cca610988565b5b610cd787828801610c1b565b925092505092959194509250565b6000608082019050610cfa6000830187610a30565b610d076020830186610a30565b610d146040830185610a71565b610d216060830184610a8a565b95945050505050565b600082825260208201905092915050565b7f4e6f742073797374656d2077616c6c6574000000000000000000000000000000600082015250565b6000610d71601183610d2a565b9150610d7c82610d3b565b602082019050919050565b60006020820190508181036000830152610da081610d64565b9050919050565b7f426174636820616c7265616479207265636f7264656400000000000000000000600082015250565b6000610ddd601683610d2a565b9150610de882610da7565b602082019050919050565b60006020820190508181036000830152610e0c81610dd0565b9050919050565b6000604082019050610e286000830185610a30565b610e356020830184610a8a565b9392505050565b82818337600083830152505050565b6000601f19601f8301169050919050565b6000610e688385610d2a565b9350610e75838584610e3c565b610e7e83610e4b565b840190509392505050565b60006060820190508181036000830152610ea4818789610e5c565b90508181036020830152610eb9818587610e5c565b9050610ec86040830184610a8a565b9695505050505050565b7f5472616e73666f726d656420626174636820616c7265616479207265636f726460008201527f6564000000000000000000000000000000000000000000000000000000000000602082015250565b6000610f2e602283610d2a565b9150610f3982610ed2565b604082019050919050565b60006020820190508181036000830152610f5d81610f21565b9050919050565b600081905092915050565b600080fd5b82818337505050565b6000610f898385610f64565b93507f07ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff831115610fbc57610fbb610f6f565b5b602083029250610fcd838584610f74565b82840190509392505050565b6000610fe6828486610f7d565b91508190509392505050565b60006060820190506110076000830186610a30565b6110146020830185610a30565b6110216040830184610a8a565b94935050505056fea2646970667358221220e0173d04f97448a56c7e5740c1575ab3961e7f95cdf5bf063d51e209756b9bde64736f6c63430008130033";

    public static final String FUNC_RECORDBATCH = "recordBatch";
    public static final String FUNC_RECORDTRANSFORMEDBATCH = "recordTransformedBatch";
    public static final String FUNC_GETBATCHRECORD = "getBatchRecord";
    public static final String FUNC_GETTRANSFORMEDBATCHRECORD = "getTransformedBatchRecord";
    public static final String FUNC_LOGOWNERSHIPCHANGE = "logOwnershipChange";
    public static final String FUNC_HASBATCH = "hasBatch";
    public static final String FUNC_HASTRANSFORMEDBATCH = "hasTransformedBatch";

    @Deprecated
    protected Traceability(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice,
            BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Traceability(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Traceability(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Traceability(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> logOwnershipChange(
            byte[] _batchId, String _fromUserId, String _toUserId) {
        final Function function = new Function(
                FUNC_LOGOWNERSHIPCHANGE,
                Arrays.<Type>asList(
                        new Bytes32(_batchId),
                        new Utf8String(_fromUserId),
                        new Utf8String(_toUserId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    /** Ghi lô gốc (RAW). _batchId và _dataHash phải là mảng 32 byte. */
    public RemoteFunctionCall<TransactionReceipt> recordBatch(byte[] _batchId, byte[] _dataHash) {
        final Function function = new Function(
                FUNC_RECORDBATCH,
                Arrays.<Type>asList(new Bytes32(_batchId), new Bytes32(_dataHash)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    /**
     * Ghi Pallet / lô chế biến. Mỗi phần tử trong _parentHashes phải là 32 byte.
     * <p>Web3j 4.x: {@code DynamicArray} rỗng khi encode ABI có thể gây {@code Index 0 out of bounds for length 0}.
     * Nếu không có parent, gửi một bytes32 toàn 0 — trên contract parentRoot = keccak256(pack(0x00..00)).</p>
     */
    public RemoteFunctionCall<TransactionReceipt> recordTransformedBatch(
            byte[] _batchId, byte[] _dataHash, List<byte[]> _parentHashes) {
        final List<Bytes32> parentList;
        if (_parentHashes == null || _parentHashes.isEmpty()) {
            parentList = Collections.singletonList(new Bytes32(new byte[32]));
        } else {
            parentList = _parentHashes.stream()
                    .map(Bytes32::new)
                    .collect(Collectors.toList());
        }
        final Function function = new Function(
                FUNC_RECORDTRANSFORMEDBATCH,
                Arrays.<Type>asList(
                        new Bytes32(_batchId),
                        new Bytes32(_dataHash),
                        new DynamicArray<>(Bytes32.class, parentList)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    /** getBatchRecord(bytes32) -> (dataHash, actor, timestamp) */
    public RemoteFunctionCall<Tuple3<byte[], String, BigInteger>> getBatchRecord(byte[] _batchId) {
        final Function function = new Function(
                FUNC_GETBATCHRECORD,
                Arrays.<Type>asList(new Bytes32(_batchId)),
                Arrays.<TypeReference<?>>asList(
                        new TypeReference<Bytes32>() {
                        },
                        new TypeReference<Address>() {
                        },
                        new TypeReference<Uint256>() {
                        }));
        return new RemoteFunctionCall<>(function,
                () -> {
                    List<Type> results = executeCallMultipleValueReturn(function);
                    byte[] dataHash = ((Bytes32) results.get(0)).getValue();
                    String actor = results.get(1).getValue().toString();
                    BigInteger timestamp = (BigInteger) results.get(2).getValue();
                    return new Tuple3<>(dataHash, actor, timestamp);
                });
    }

    /**
     * getTransformedBatchRecord(bytes32) -> (dataHash, parentRoot, actor,
     * timestamp)
     */
    public RemoteFunctionCall<Tuple4<byte[], byte[], String, BigInteger>> getTransformedBatchRecord(byte[] _batchId) {
        final Function function = new Function(
                FUNC_GETTRANSFORMEDBATCHRECORD,
                Arrays.<Type>asList(new Bytes32(_batchId)),
                Arrays.<TypeReference<?>>asList(
                        new TypeReference<Bytes32>() {
                        },
                        new TypeReference<Bytes32>() {
                        },
                        new TypeReference<Address>() {
                        },
                        new TypeReference<Uint256>() {
                        }));
        return new RemoteFunctionCall<>(function,
                () -> {
                    List<Type> results = executeCallMultipleValueReturn(function);
                    byte[] dataHash = ((Bytes32) results.get(0)).getValue();
                    byte[] parentRoot = ((Bytes32) results.get(1)).getValue();
                    String actor = results.get(2).getValue().toString();
                    BigInteger timestamp = (BigInteger) results.get(3).getValue();
                    return new Tuple4<>(dataHash, parentRoot, actor, timestamp);
                });
    }

    public RemoteFunctionCall<Boolean> hasBatch(byte[] _batchId) {
        final Function function = new Function(
                FUNC_HASBATCH,
                Arrays.<Type>asList(new Bytes32(_batchId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<org.web3j.abi.datatypes.Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> hasTransformedBatch(byte[] _batchId) {
        final Function function = new Function(
                FUNC_HASTRANSFORMEDBATCH,
                Arrays.<Type>asList(new Bytes32(_batchId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<org.web3j.abi.datatypes.Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static Traceability load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice,
            BigInteger gasLimit) {
        return new Traceability(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Traceability load(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new Traceability(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Traceability load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new Traceability(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Traceability load(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        return new Traceability(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    /**
     * ABI-encode constructor(address _systemWallet) — dùng cho mọi overload deploy.
     */
    private static String encodeConstructorSystemWallet(String systemWalletAddress) {
        return FunctionEncoder.encodeConstructor(
                Arrays.<Type>asList(new Address(160, systemWalletAddress)));
    }

    public static RemoteCall<Traceability> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, String systemWalletAddress) {
        return deployRemoteCall(Traceability.class, web3j, credentials, contractGasProvider, BINARY,
                encodeConstructorSystemWallet(systemWalletAddress));
    }

    @Deprecated
    public static RemoteCall<Traceability> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice,
            BigInteger gasLimit, String systemWalletAddress) {
        return deployRemoteCall(Traceability.class, web3j, credentials, gasPrice, gasLimit, BINARY,
                encodeConstructorSystemWallet(systemWalletAddress));
    }

    @Deprecated
    public static RemoteCall<Traceability> deploy(Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit, String systemWalletAddress) {
        return deployRemoteCall(Traceability.class, web3j, transactionManager, gasPrice, gasLimit, BINARY,
                encodeConstructorSystemWallet(systemWalletAddress));
    }

    public static RemoteCall<Traceability> deploy(Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider, String systemWalletAddress) {
        return deployRemoteCall(Traceability.class, web3j, transactionManager, contractGasProvider, BINARY,
                encodeConstructorSystemWallet(systemWalletAddress));
    }
}

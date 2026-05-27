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
    public static final String BINARY = "60a060405234801561001057600080fd5b5060405161089138038061089183398101604081905261002f91610067565b6001600160a01b038116610056576040516317762fc560e31b815260040160405180910390fd5b6001600160a01b0316608052610097565b60006020828403121561007957600080fd5b81516001600160a01b038116811461009057600080fd5b9392505050565b6080516107bc6100d56000396000818161014f015281816101e9015281816102f40152818161033d015281816103cd015261053401526107bc6000f3fe608060405234801561001057600080fd5b50600436106100885760003560e01c8063a0a0df3d1161005b578063a0a0df3d14610125578063ad7d3a721461014a578063b18dce5814610189578063e2e18b161461019c57600080fd5b8063173fd0271461008d5780631b9ce039146100a25780633ee02e7b146100dd57806342752fa614610112575b600080fd5b6100a061009b366004610579565b6101de565b005b6100b56100b036600461059b565b6102c1565b604080519384526001600160a01b039092166020840152908201526060015b60405180910390f35b6101026100eb36600461059b565b600090815260016020526040902060020154151590565b60405190151581526020016100d4565b6100a06101203660046105fd565b610332565b61010261013336600461059b565b600090815260208190526040902060010154151590565b6101717f000000000000000000000000000000000000000000000000000000000000000081565b6040516001600160a01b0390911681526020016100d4565b6100a0610197366004610677565b6103c2565b6101af6101aa36600461059b565b6104f0565b6040516100d4949392919093845260208401929092526001600160a01b03166040830152606082015260800190565b336001600160a01b037f0000000000000000000000000000000000000000000000000000000000000000161461022757604051630a909e5d60e41b815260040160405180910390fd5b6000828152602081905260409020600101541561025757604051631362144160e01b815260040160405180910390fd5b60408051808201825282815242602080830182815260008781528083528590209351845551600190930192909255825184815291820152339184917f9625ae2ab8825c8b6070e01fc994e3ff9b0acde223ee402568856d02170b1e62910160405180910390a35050565b600081815260208181526040808320815180830190925280548252600101549181018290528291829190829015610318577f000000000000000000000000000000000000000000000000000000000000000061031b565b60005b825160209093015192979096509194509092505050565b336001600160a01b037f0000000000000000000000000000000000000000000000000000000000000000161461037b57604051630a909e5d60e41b815260040160405180910390fd5b847fd4feedd7eb1e263f0f1c630099fe7e3a4bd82b2ed1359ab9b75e0ca3af330af685858585426040516103b3959493929190610723565b60405180910390a25050505050565b336001600160a01b037f0000000000000000000000000000000000000000000000000000000000000000161461040b57604051630a909e5d60e41b815260040160405180910390fd5b6000848152600160205260409020600201541561043b57604051630f0e405160e31b815260040160405180910390fd5b6000828260405160200161045092919061075d565b60408051808303601f190181528282528051602091820120606080850184528885528285018281524286860181815260008d8152600180885290889020985189559251928801929092559051600290960195909555835189815292830182905292820193909352919250339187917f232e79352ac0dba2afb4cb6f5a053fa184ed21d6875c42d1a696fb4f9641a706910160405180910390a35050505050565b6000818152600160208181526040808420815160608101835281548152938101549284019290925260029091015490820181905282918291829190829015610558577f000000000000000000000000000000000000000000000000000000000000000061055b565b60005b82516020840151604090940151909993985090965094509092505050565b6000806040838503121561058c57600080fd5b50508035926020909101359150565b6000602082840312156105ad57600080fd5b5035919050565b60008083601f8401126105c657600080fd5b50813567ffffffffffffffff8111156105de57600080fd5b6020830191508360208285010111156105f657600080fd5b9250929050565b60008060008060006060868803121561061557600080fd5b85359450602086013567ffffffffffffffff8082111561063457600080fd5b61064089838a016105b4565b9096509450604088013591508082111561065957600080fd5b50610666888289016105b4565b969995985093965092949392505050565b6000806000806060858703121561068d57600080fd5b8435935060208501359250604085013567ffffffffffffffff808211156106b357600080fd5b818701915087601f8301126106c757600080fd5b8135818111156106d657600080fd5b8860208260051b85010111156106eb57600080fd5b95989497505060200194505050565b81835281816020850137506000828201602090810191909152601f909101601f19169091010190565b6060815260006107376060830187896106fa565b828103602084015261074a8186886106fa565b9150508260408301529695505050505050565b60006001600160fb1b0383111561077357600080fd5b8260051b8085843791909101939250505056fea26469706673582212203a77596cec5804c5dd8c00c459fc2dbae2ed616eb7290c3d1679d216448b19b764736f6c63430008130033";

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

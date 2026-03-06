package vn.edu.kma.blockchain_service.contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;


@SuppressWarnings("rawtypes")
public class Traceability extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50610a85806100206000396000f3fe608060405234801561001057600080fd5b506004361061004c5760003560e01c80631617f59f146100515780635f4742dd14610077578063d052fbf61461009b578063dcc77ce6146100ae575b600080fd5b61006461005f3660046106fc565b6100c3565b6040519081526020015b60405180910390f35b61008a610085366004610739565b6100ea565b60405161006e9594939291906107ce565b61008a6100a9366004610739565b6102ee565b6100c16100bc36600461082a565b61053f565b005b600080826040516100d491906108b2565b9081526040519081900360200190205492915050565b8151602081840181018051600082529282019185019190912091905280548290811061011557600080fd5b906000526020600020906005020160009150915050806000018054610139906108ce565b80601f0160208091040260200160405190810160405280929190818152602001828054610165906108ce565b80156101b25780601f10610187576101008083540402835291602001916101b2565b820191906000526020600020905b81548152906001019060200180831161019557829003601f168201915b5050505050908060010180546101c7906108ce565b80601f01602080910402602001604051908101604052809291908181526020018280546101f3906108ce565b80156102405780601f1061021557610100808354040283529160200191610240565b820191906000526020600020905b81548152906001019060200180831161022357829003601f168201915b505050505090806002018054610255906108ce565b80601f0160208091040260200160405190810160405280929190818152602001828054610281906108ce565b80156102ce5780601f106102a3576101008083540402835291602001916102ce565b820191906000526020600020905b8154815290600101906020018083116102b157829003601f168201915b5050505060038301546004909301549192916001600160a01b0316905085565b60608060606000806000808860405161030791906108b2565b9081526020016040518091039020878154811061032657610326610908565b90600052602060002090600502016040518060a001604052908160008201805461034f906108ce565b80601f016020809104026020016040519081016040528092919081815260200182805461037b906108ce565b80156103c85780601f1061039d576101008083540402835291602001916103c8565b820191906000526020600020905b8154815290600101906020018083116103ab57829003601f168201915b505050505081526020016001820180546103e1906108ce565b80601f016020809104026020016040519081016040528092919081815260200182805461040d906108ce565b801561045a5780601f1061042f5761010080835404028352916020019161045a565b820191906000526020600020905b81548152906001019060200180831161043d57829003601f168201915b50505050508152602001600282018054610473906108ce565b80601f016020809104026020016040519081016040528092919081815260200182805461049f906108ce565b80156104ec5780601f106104c1576101008083540402835291602001916104ec565b820191906000526020600020905b8154815290600101906020018083116104cf57829003601f168201915b505050918352505060038201546020808301919091526004909201546001600160a01b0316604091820152825191830151908301516060840151608090940151929c919b50995091975095509350505050565b60008360405161054f91906108b2565b90815260408051918290036020908101832060a08401835286845283820186905291830184905242606084015233608084015281546001810183556000928352912082516005909202019081906105a6908261096d565b50602082015160018201906105bb908261096d565b50604082015160028201906105d0908261096d565b5060608201516003820155608090910151600490910180546001600160a01b0319166001600160a01b0390921691909117905560405133906106139085906108b2565b60405180910390207f5662acac182ff25e616b76c84ee576ac1ce3e0f49717ab1053e5a71bf3753855844260405161064c929190610a2d565b60405180910390a3505050565b634e487b7160e01b600052604160045260246000fd5b600082601f83011261068057600080fd5b813567ffffffffffffffff8082111561069b5761069b610659565b604051601f8301601f19908116603f011681019082821181831017156106c3576106c3610659565b816040528381528660208588010111156106dc57600080fd5b836020870160208301376000602085830101528094505050505092915050565b60006020828403121561070e57600080fd5b813567ffffffffffffffff81111561072557600080fd5b6107318482850161066f565b949350505050565b6000806040838503121561074c57600080fd5b823567ffffffffffffffff81111561076357600080fd5b61076f8582860161066f565b95602094909401359450505050565b60005b83811015610799578181015183820152602001610781565b50506000910152565b600081518084526107ba81602086016020860161077e565b601f01601f19169290920160200192915050565b60a0815260006107e160a08301886107a2565b82810360208401526107f381886107a2565b9050828103604084015261080781876107a2565b606084019590955250506001600160a01b03919091166080909101529392505050565b60008060006060848603121561083f57600080fd5b833567ffffffffffffffff8082111561085757600080fd5b6108638783880161066f565b9450602086013591508082111561087957600080fd5b6108858783880161066f565b9350604086013591508082111561089b57600080fd5b506108a88682870161066f565b9150509250925092565b600082516108c481846020870161077e565b9190910192915050565b600181811c908216806108e257607f821691505b60208210810361090257634e487b7160e01b600052602260045260246000fd5b50919050565b634e487b7160e01b600052603260045260246000fd5b601f82111561096857600081815260208120601f850160051c810160208610156109455750805b601f850160051c820191505b8181101561096457828155600101610951565b5050505b505050565b815167ffffffffffffffff81111561098757610987610659565b61099b8161099584546108ce565b8461091e565b602080601f8311600181146109d057600084156109b85750858301515b600019600386901b1c1916600185901b178555610964565b600085815260208120601f198616915b828110156109ff578886015182559484019460019091019084016109e0565b5085821015610a1d5787850151600019600388901b60f8161c191681555b5050505050600190811b01905550565b604081526000610a4060408301856107a2565b9050826020830152939250505056fea2646970667358221220b469204f755f2fff21c79253cf903738305fcf30dda00e5765154e1554bd179f64736f6c63430008130033";

    public static final String FUNC_ADDHISTORY = "addHistory";

    public static final String FUNC_GETHISTORYCOUNT = "getHistoryCount";

    public static final String FUNC_GETHISTORY = "getHistory";

    public static final Event HISTORYADDED_EVENT = new Event("HistoryAdded",
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected Traceability(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Traceability(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Traceability(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Traceability(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> addHistory(String _productId, String _action, String _description) {
        final Function function = new Function(
                FUNC_ADDHISTORY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_productId),
                        new org.web3j.abi.datatypes.Utf8String(_action),
                        new org.web3j.abi.datatypes.Utf8String(_description)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> getHistoryCount(String _productId) {
        final Function function = new Function(FUNC_GETHISTORYCOUNT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_productId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    @Deprecated
    public static Traceability load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Traceability(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Traceability load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Traceability(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Traceability load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Traceability(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Traceability load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Traceability(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Traceability> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Traceability.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Traceability> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice,
                                                   BigInteger gasLimit) {
        return deployRemoteCall(Traceability.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Traceability> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Traceability.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<Traceability> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Traceability.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }
}
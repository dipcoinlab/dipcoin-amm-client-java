# dipcoin-amm-client-java
Java Implementation of the dipcoin AMM Swap Client Library


## Quick Start


### Maven Dependency

```xml
<dependency>
    <groupId>io.dipcoin</groupId>
    <artifactId>sui-amm-client-java</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Gradle Dependency

```gradle
implementation 'io.dipcoin:sui-amm-client-java:1.0.2'
```

### Initialize SDK

```java
import io.dipcoin.sui.amm.client.AmmClient;
import io.dipcoin.sui.amm.constant.AmmNetwork;
import io.dipcoin.sui.protocol.SuiClient;
import io.dipcoin.sui.protocol.SuiService;

public class Test{
    public static void main(String[] args) {
        // Initialize for mainnet
        HttpService suiService = new HttpService("https://fullnode.mainnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.MAINNET, suiClient);

        // Initialize for testnet
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);
    }
}
```

### AmmOffSignClient

**Purpose**: Handles on-chain operations with external wallet integration. Designed for scenarios where private keys are managed by external wallet systems (hardware wallets, wallet SDKs, custody solutions). Requires implementing the `WalletService` interface.

#### WalletService Interface

You must implement this interface to integrate with your wallet system:

```java
import io.dipcoin.sui.amm.client.chain.WalletService;

public class MyWalletService implements WalletService {
    
    @Override
    public String sign(String address, byte[] txData) {
        // Implement your wallet signing logic here
        // txData is the BCS-encoded transaction bytes
        // Return the signature string
        
        // Example with hardware wallet:
        // HardwareWallet wallet = getWalletForAddress(address);
        // byte[] signature = wallet.signTransaction(txData);
        // return Base64.toBase64String(signature);
        
        // Example with key management service:
        // KeyManagementService kms = getKMSClient();
        // return kms.signTransaction(address, txData);
        
        return yourSigningImplementation(address, txData);
    }
}
```

## Core Features

### Liquidity Operations

#### Add Liquidity

Add liquidity to an existing pool with slippage protection:

```java
public class Test{
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);
        
        AddLiquidityParams params = new AddLiquidityParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setAmountX(BigInteger.valueOf(1000000L));
        params.setAmountY(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.addLiquidity(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/8XKvP3zqdEfo5CAaDwcpCkehkzoZ1mc8yGjnZLdJWV2U
        log.info("Response: {}", response);
    }
}
```

#### Remove Liquidity

Remove liquidity from a pool:

```java
public class Test{
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);

        RemoveLiquidityParams params = new RemoveLiquidityParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setRemoveLpAmount(BigInteger.valueOf(1000000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.removeLiquidity(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/81bDEnXXVJZBGiH1wBKHqYxDmeb7rGqzdjXwDKbxyNVr
        log.info("Response: {}", response);
    }
}
```

### Swap Operations

#### Swap Exact Input

Swap an exact amount of input tokens for output tokens:

```java
public class Test{
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);

        SwapParams params = new SwapParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountIn(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.swapExactXToY(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/ssBddYuiuNuTsGT94oy39pPiyYkrNJvuwbrQAXYmhwx
        log.info("Response: {}", response);
    }
}
```

#### Swap Exact Output

Swap tokens for an exact amount of output tokens:

```java
public class Test{
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);

        SwapParams params = new SwapParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountOut(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.swapXToExactY(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/CnAtv9CNe8khoaQrxHdishREc6ZX7sMosA3zsVnwPbBT
        log.info("Response: {}", response);
    }
}
```

### Query Functions

#### Get Pool Information

```java
public class Test{
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);

        Pool pool = ammClient.getPool("YOUR_POOL_ID");
        log.info("pool: {}", pool);
    }
}
```

#### Get Pool ID

Get pool ID for a token pair:

```java
public class Test{
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);

        String poolId = ammClient.getPoolId("0x...::usdc::USDC", "0x...::wsol::WSOL");
        log.info("poolId: {}", poolId);
    }
}
```

#### Get Global Configuration

```java
public class Test{
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);

        Global global = ammClient.getGlobal();
        log.info("global: {}", global);
    }
}
```

### Split Coins

Split a specified amount from available coins. This is typically used internally by the SDK but can also be used directly if needed:

```java
public class Test {
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);

        // split is not a Sui coin
        String address = "0x123";
        String coinType = "0x...::usdc::USDC";
        BigInteger amount = new BigInteger("1000");
        ProgrammableTransaction programmableTx = new ProgrammableTransaction();
        int index = ammClient.splitCoin(programmableTx, address, coinType, amount);
        log.info("programmableTx: {}", programmableTx);

        // split Sui
        BigInteger amount = new BigInteger("1000");
        ProgrammableTransaction programmableTx = new ProgrammableTransaction();
        int index = ammClient.splitSui(programmableTx, amount);
        log.info("programmableTx: {}", programmableTx);
    }
}
```

The method will:

1. Query available coins of the specified type
2. Merge multiple coins if necessary
3. Split the requested amount
4. Return the split coin reference

Example usage within a custom transaction:

```java
public class Test {
    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443"); // Optional custom RPC
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);
        String address = suiKeyPair.address();

        BigInteger amountIn = params.getAmountIn();
        BigInteger slippage = params.getSlippage();

        String poolId = params.getPoolId();
        Pool pool = ammClient.getPool(params.getPoolId());

        String typeX = params.getTypeX();
        String[] orderType = PackageUtil.orderType(typeX, params.getTypeY());
        boolean isSwap = !orderType[0].equals(typeX);
        BigInteger balanceX = isSwap ? pool.getBalY() : pool.getBalX();
        BigInteger balanceY = isSwap ? pool.getBalX() : pool.getBalY();
        BigInteger amountOut = MathUtil.getAmountOut(pool.getFeeRate(), amountIn, balanceX, balanceY);

        BigInteger amountOutMin = MathUtil.getSlippageAmount(amountOut, slippage);
        String functionName = isSwap ? SwapConstant.SWAP_EXACT_Y_TO_X : SwapConstant.SWAP_EXACT_X_TO_Y;

        ProgrammableTransaction programmableTx = new ProgrammableTransaction();

        int splitIndex = 0;
        AtomicReference<BigInteger> suiUse = new AtomicReference<>(BigInteger.ZERO);
        if (typeX.equals(SwapConstant.COIN_TYPE_SUI)) {
            // split Sui
            splitIndex = ammClient.splitSui(programmableTx, amountIn);
            suiUse.set(amountIn);
        } else {
            // split is not a Sui coin
            splitIndex = ammClient.splitCoin(programmableTx, address, typeX, amountIn);
        }

        List<TypeTag> typeTags = new ArrayList<>(2);
        typeTags.add(TypeTagSerializer.parseFromStr(orderType[0], true));
        typeTags.add(TypeTagSerializer.parseFromStr(orderType[1], true));

        ProgrammableMoveCall moveCall = new ProgrammableMoveCall(
                ammConfig.packageId(),
                MODULE,
                functionName,
                typeTags,
                Arrays.asList(
                        Argument.ofInput(programmableTx.addInput(ammClient.getSharedObject(ammClient.ammConfig.globalId(), false))),
                        Argument.ofInput(programmableTx.addInput(ammClient.getSharedObject(poolId, true))),
                        new Argument.NestedResult(splitIndex, 0),
                        Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(amountOutMin.longValue(), PureBcs.BasePureType.U64)))
                )
        );

        Command depositMoveCallCommand = new Command.MoveCall(moveCall);
        List<Command> commands = new ArrayList<>(List.of(
                depositMoveCallCommand
        ));
        programmableTx.addCommands(commands);

        try {
            // Sign and execute the transaction
            return TransactionBuilder.sendTransaction(suiClient, programmableTx, suiKeyPair, TransactionBuilder.buildGasData(suiClient, address, gasPrice, gasBudget, suiUse.get()));
        } catch (IOException e) {
            throw new AmmException(e.getMessage());
        }
    }
}
```

Error handling:

- Throws if no coins are available
- Throws if total balance is insufficient
- Throws if coin operations fail

## Types

### Pool Interface

```java
public class Pool {
    
    /** Pool ID */
    private String id;
    
    /** Token X balance */
    @JsonProperty("bal_x")
    private BigInteger balX;
    
    /** Token Y balance */
    @JsonProperty("bal_y")
    private BigInteger balY;

    /** Accumulated fee balance for token X */
    @JsonProperty("fee_bal_x")
    private BigInteger feeBalX;

    /** Accumulated fee balance for token Y */
    @JsonProperty("fee_bal_y")
    private BigInteger feeBalY;

    /** Total LP token supply */
    @JsonProperty("lp_supply")
    private BigInteger lpSupply;

    /** Pool fee rate */
    @JsonProperty("fee_rate")
    private BigInteger feeRate;

    /** Minimum liquidity required when first adding liquidity,will leave 1000 */
    @JsonProperty("min_liquidity")
    private BigInteger minLiquidity;

    /** Minimum LP amount required for adding liquidity */
    @JsonProperty("min_add_liquidity_lp_amount")
    private BigInteger minAddLiquidityLpAmount;

}
```

### Global Interface

```java
public class Global {

    /** Global config ID */
    private String id;

    /** Whether protocol is paused */
    @JsonProperty("has_paused")
    private boolean hasPaused;

    /** Whether protocol fee is enabled */
    @JsonProperty("is_open_protocol_fee")
    private boolean isOpenProtocolFee;

}
```

## Constants

### Default Values

- Default slippage tolerance: 5% `new BigInteger("500")`
- Maximum fee rate: 1% (BigInteger.ONE)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
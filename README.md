# BPSWallet

BPSWallet is an offline Bitcoin wallet used to securely manage keys and create transactions.

### Features
  - AES/ECIES file encryption for secure on-disk wallet storage and access
  - Sensitive information is in program memory only when in use and promptly overwritten
  - Accurate transaction fee calculations
  - Testing of transaction creation and verification by generating fake UTXOs

### Installation

BPSWallet requires Java 8 or higher.

Head to the [releases](https://github.com/Septem151/BPSWallet/releases) section for the executable .jar file. To start BPSWallet, run the following command:

```sh
$ cd installed/location
$ java -jar BPSWallet.jar
```


package bpswallet.util;

import bpswallet.wallet.BPSWallet;
import bpswallet.txn.Transaction;
import bpswallet.crypto.ECIESData;
import bpswallet.crypto.InvalidPasswordException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.net.URL;
import java.io.RandomAccessFile;

public final class FileUtil {

    public static final String ROOT_DIR
            = (System.getProperty("os.name").startsWith("Windows")
            ? System.getenv("SystemDrive") + "/" : System.getProperty("user.home") + "/");
    public static final String DATA_DIR = (System.getProperty("os.name").startsWith("Windows")
            ? ROOT_DIR + "BPSWallet/" : ROOT_DIR + ".bpswallet/");
    public static final String WALLETS_DIR = DATA_DIR + "wallets/";
    public static final String TXNS_DIR = DATA_DIR + "txns/";
    public static final String ETC_DIR = DATA_DIR + "etc/";
    public static final String BIP39WORDS_PATH = ETC_DIR + "bip39words.txt";
    public static final String TXINDEX_PATH = ETC_DIR + "txindex.dat";
    public static final String TXN_PREFIX = "txn";
    public static final String BIP39WORDS_URL = "https://raw.githubusercontent.com/bitcoin/bips/master/bip-0039/english.txt";
    private static ArrayList<String> bip39words;
    private static HashMap<String, TxIndex> txIndex;

    public static HashMap<String, TxIndex> getTxIndex() {
        if (txIndex == null) {
            loadTxIndex();
        }
        return txIndex;
    }

    public static String[] listAllTransactions(boolean littleEndian) {
        if (txIndex == null) {
            loadTxIndex();
        }
        String[] txns = new String[txIndex.size()];
        int i = 0;
        for (String hash : txIndex.keySet()) {
            txns[i] = (littleEndian) ? ByteUtil.flipendian(hash) : hash;
            i++;
        }
        Arrays.sort(txns);
        return txns;
    }

    public static Transaction getTransaction(String hash, boolean littleEndian) {
        String txHash = (littleEndian) ? ByteUtil.flipendian(hash) : hash;
        mkdirs();
        if (txIndex == null) {
            loadTxIndex();
        }
        if (!txIndex.containsKey(txHash)) {
            return null;
        }
        TxIndex indexObj = txIndex.get(txHash);
        try {
            File file = new File(TXNS_DIR + TXN_PREFIX + String.format("%03d", indexObj.fileNum) + ".dat");
            RandomAccessFile accessor = new RandomAccessFile(file, "r");
            accessor.seek(indexObj.pointer);
            byte[] buffer = new byte[1024 * 1024];
            int read = accessor.read(buffer);
            buffer = Arrays.copyOfRange(buffer, 0, read);
            accessor.close();
            try {
                Transaction txn = Transaction.fromBytes(buffer);
                return txn;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        } catch (IOException ex) {
            System.out.println("Transaction file(s) is missing and/or read permission is denied.");
            throw new RuntimeException(ex);
        }
    }

    public static boolean addTransaction(Transaction txn) {
        mkdirs();
        if (txIndex == null) {
            loadTxIndex();
        }
        String txHash = txn.getHash(false);
        if (txIndex.containsKey(txHash)) {
            return false;
        }
        try {
            File folder = new File(TXNS_DIR);
            File[] files = folder.listFiles();
            Arrays.sort(files);
            int fileNum = files.length;
            File file = files[fileNum - 1];
            if (file.length() + txn.getHex().length() >= 1024 * 1024 * 4) {
                fileNum++;
                file = new File(TXNS_DIR + TXN_PREFIX + String.format("%03d", fileNum) + ".dat");
                if (!file.createNewFile()) {
                    throw new IOException("File already exists or permission is denied.");
                }
            }
            RandomAccessFile accessor = new RandomAccessFile(file, "rw");
            long pointer = accessor.length();
            accessor.seek(pointer);
            accessor.write(txn.getBytes());
            accessor.close();
            file = new File(TXINDEX_PATH);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException("Permission denied.");
                }
            }
            accessor = new RandomAccessFile(file, "rw");
            accessor.seek(accessor.length());
            accessor.write(ByteUtil.int2bytes(fileNum, false));
            accessor.write(ByteUtil.long2bytes(pointer, false));
            accessor.write(ByteUtil.hex2bytes(txHash));
            accessor.close();

            if (txIndex == null) {
                loadTxIndex();
            }
            txIndex.put(txHash, new TxIndex(fileNum, pointer));
            return true;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean addTransactionsFromFile(String fileName, BPSWallet wallet) {
        File file = new File(DATA_DIR + "imports/" + fileName);
        file.mkdirs();
        if (file.exists()) {
            try {
                for (String rawtxn : Files.readAllLines(file.toPath())) {
                    Transaction txn = Transaction.fromHex(rawtxn);
                    if (addTransaction(txn)) {
                        System.out.println("Transaction imported: " + txn.getHash(true));
                        wallet.addTransaction(txn);
                    }
                }
            } catch (IOException ex) {
                System.out.println("File exception when adding transactions from file.");
                return false;
            }
        }
        return true;
    }

    private static void loadTxIndex() {
        mkdirs();
        txIndex = new HashMap<>();
        try {
            File file = new File(TXINDEX_PATH);
            if (!file.exists()) {
                reindex();
            } else {
                RandomAccessFile accessor = new RandomAccessFile(file, "r");
                byte[] buffer = new byte[1024 * 44];
                int read;
                while ((read = accessor.read(buffer)) > 0) {
                    String hex = ByteUtil.hexify(Arrays.copyOfRange(buffer, 0, read));
                    for (int i = 0; i < hex.length(); i += 88) {
                        int fileNum = ByteUtil.hex2int(hex.substring(i, i + 8), false);
                        long pointer = ByteUtil.hex2long(hex.substring(i + 8, i + 24), false);
                        File txnFile = new File(TXNS_DIR + TXN_PREFIX + String.format("%03d", fileNum) + ".dat");
                        if (!txnFile.exists()) {
                            accessor.close();
                            reindex();
                            return;
                        } else if (txnFile.length() <= pointer) {
                            accessor.close();
                            reindex();
                            return;
                        } else {
                            String txHash = hex.substring(i + 24, i + 88);
                            txIndex.put(txHash, new TxIndex(fileNum, pointer));
                        }
                    }
                }
                accessor.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void reindex() {
        mkdirs();
        try {
            File folder = new File(TXNS_DIR);
            File[] files = folder.listFiles();
            Arrays.sort(files);
            File indexFile = new File(TXINDEX_PATH);
            if (indexFile.exists()) {
                indexFile.delete();
            }
            indexFile.createNewFile();
            RandomAccessFile indexAccessor = new RandomAccessFile(indexFile, "rw");
            for (int fileNum = 1; fileNum <= files.length; fileNum++) {
                File file = files[fileNum - 1];
                RandomAccessFile accessor = new RandomAccessFile(file, "r");
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = accessor.read(buffer)) > 0) {
                    byte[] bytes = Arrays.copyOfRange(buffer, 0, read);
                    try {
                        Transaction txn = Transaction.fromBytes(bytes);
                        String txHash = txn.getHash(false);
                        long pointer = accessor.getFilePointer() - bytes.length;
                        accessor.seek(pointer + txn.getHex().length() / 2);
                        indexAccessor.writeInt(fileNum);
                        indexAccessor.writeLong(pointer);
                        indexAccessor.write(ByteUtil.hex2bytes(txHash));
                    } catch (Exception ex) {
                        accessor.close();
                        throw new IOException(ex);
                    }
                }
                accessor.close();
            }
            indexAccessor.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String[] listWalletFileNames() {
        mkdirs();
        File folder = new File(WALLETS_DIR);
        File[] files = folder.listFiles();
        Arrays.sort(files);
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String fileName = file.getName().replace(".dat", "");
            fileNames[i] = fileName;
        }
        return fileNames;
    }

    public static boolean walletFileExists(String name) {
        mkdirs();
        File walletFile = new File(WALLETS_DIR + name + ".dat");
        return walletFile.exists();
    }

    public static boolean walletFileIsEncrypted(String name) {
        mkdirs();
        File walletFile = new File(WALLETS_DIR + name + ".dat");
        try {
            RandomAccessFile accessor = new RandomAccessFile(walletFile, "r");
            byte flag = accessor.readByte();
            accessor.close();
            return flag == 0x01;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void writeWalletToFile(BPSWallet wallet) {
        mkdirs();
        byte[] encryptedData = wallet.getEncrypted().getFileFormat();
        try {
            File file = new File(WALLETS_DIR + wallet.getFileName() + ".dat");
            if (!file.exists()) {
                file.createNewFile();
            }
            RandomAccessFile accessor = new RandomAccessFile(file, "rw");
            accessor.write(encryptedData);
            accessor.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static BPSWallet loadWalletFromFile(String fileName, char[] password) throws InvalidPasswordException {
        mkdirs();
        File file = new File(WALLETS_DIR + fileName + ".dat");
        try {
            String fileData = ByteUtil.hexify(Files.readAllBytes(file.toPath()));
            ECIESData encryptedData = new ECIESData(fileData);
            return new BPSWallet(fileName, encryptedData, password);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ArrayList<String> getBIP39Words() {
        if (bip39words == null) {
            try {
                File file = new File(BIP39WORDS_PATH);
                if (!file.exists()) {
                    mkdirs();
                    file.createNewFile();
                    URL url = new URL(BIP39WORDS_URL);
                    ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    FileChannel fileChannel = fileOutputStream.getChannel();
                    fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    fileOutputStream.close();
                }
                bip39words = new ArrayList<>(Files.readAllLines(file.toPath()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return bip39words;
    }

    private static void mkdirs() {
        File folder = new File(WALLETS_DIR);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        folder = new File(TXNS_DIR);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(TXNS_DIR + TXN_PREFIX + String.format("%03d", 1) + ".dat");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        folder = new File(ETC_DIR);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public static class TxIndex {

        public int fileNum;
        public long pointer;

        public TxIndex(int fileNum, long pointer) {
            this.fileNum = fileNum;
            this.pointer = pointer;
        }
    }
}

package bpswallet.txn;

import bpswallet.wallet.BPSWallet;
import bpswallet.ser.Address;
import bpswallet.ser.AddressType;
import bpswallet.ser.VarInt;
import bpswallet.crypto.ECKeyPair;
import bpswallet.crypto.ECPubKey;
import bpswallet.crypto.ECSignature;
import bpswallet.util.ByteUtil;
import bpswallet.util.FileUtil;
import bpswallet.util.HashUtil;
import java.util.ArrayList;
import java.util.Objects;

public class Transaction {

    public static final int SIGHASH_ALL = 1;

    public static final int DEFAULT_VERSION = 1;
    public static final int DEFAULT_LOCKTIME = 0;
    public static final int SEGWIT_MARKER = 0;
    public static final int SEGWIT_FLAG = 1;

    private int version, marker, flag;
    private final ArrayList<TransactionInput> inputs;
    private final ArrayList<TransactionOutput> outputs;
    private final ArrayList<WitnessProgram> witness;
    private int locktime;

    public Transaction() {
        version = DEFAULT_VERSION;
        marker = SEGWIT_MARKER;
        flag = SEGWIT_FLAG;
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        witness = new ArrayList<>();
        locktime = DEFAULT_LOCKTIME;
    }

    public boolean sign(ArrayList<ECKeyPair> keyPairs) {
        if (keyPairs.size() != inputs.size()) {
            return false;
        }
        boolean result = true;
        for (int i = 0; i < inputs.size(); i++) {
            result = result && signSingleInput(i, keyPairs.get(i));
        }
        if (!result) {
            this.clearInputScriptSigs();
            this.clearWitness();
        }
        return result;
    }

    public boolean signSingleInput(int index, ECKeyPair keyPair) {
        TransactionInput input = inputs.get(index);
        Outpoint outpoint = input.getOutpoint();
        Transaction refTxn = FileUtil.getTransaction(outpoint.getHash(), false);
        TransactionOutput refOutput = refTxn.getOutputAt(outpoint.getIndex());
        Address addr = refOutput.getAddress();
        if (addr.getType() == AddressType.LEGACY) {
            String[] oldScripts = this.clearInputScriptSigs();
            input.setScriptSig(refOutput.getScriptPubKey());
            // temporarily clear witness
            ArrayList<WitnessProgram> oldWitness = this.clearWitness();
            String preimage = this.getHex() + ByteUtil.int2hex(SIGHASH_ALL, true);
            String sigHash = HashUtil.doubleSha256(preimage);
            ECSignature sig = keyPair.getPrv().sign(sigHash);
            this.setInputScriptSigs(oldScripts);
            this.setWitnessPrograms(oldWitness);
            String scriptSig = new VarInt(sig.getHex().length() / 2 + 1).toHex()
                    + sig.getHex() + String.format("%02x", SIGHASH_ALL)
                    + new VarInt(33).toHex() + keyPair.getPub().getEncoded();
            input.setScriptSig(scriptSig);
            return true;
        } else if (addr.getType() == AddressType.SEGWIT
                || addr.getType() == AddressType.BECH32) {
            String nVersion = ByteUtil.int2hex(version, true);
            String nLocktime = ByteUtil.int2hex(locktime, true);
            String nHashType = ByteUtil.int2hex(SIGHASH_ALL, true);
            String nSequence = ByteUtil.int2hex(input.getSequence(), true);
            String prevOuts = "";
            String sequences = "";
            String outs = "";
            for (TransactionInput txIn : inputs) {
                prevOuts += txIn.getOutpoint().getHex();
                sequences += ByteUtil.int2hex(txIn.getSequence(), true);
            }
            for (TransactionOutput output : this.getOutputs()) {
                outs += output.getHex();
            }
            String hashPrevouts = HashUtil.doubleSha256(prevOuts);
            String hashSequence = HashUtil.doubleSha256(sequences);
            String hashOutputs = HashUtil.doubleSha256(outs);
            ECPubKey pubKey = keyPair.getPub();
            String pubKeyHash = HashUtil.hash160(pubKey.getEncoded());
            String scriptCode = "1976a914" + pubKeyHash + "88ac";
            if (addr.getType() == AddressType.SEGWIT) {
                input.setScriptSig("160014" + pubKeyHash);
            }
            String preimage = nVersion + hashPrevouts + hashSequence
                    + outpoint.getHex() + scriptCode + ByteUtil.long2hex(refOutput.getValue(), true)
                    + nSequence + hashOutputs + nLocktime + nHashType;
            String sigHash = HashUtil.doubleSha256(preimage);
            ECSignature sig = keyPair.getPrv().sign(sigHash);
            WitnessProgram program = new WitnessProgram();
            program.addPush(sig.getHex() + String.format("%02x", SIGHASH_ALL));
            program.addPush(pubKey.getEncoded());
            witness.set(index, program);
            return true;
        } else {
            return false;
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setLocktime(int locktime) {
        this.locktime = locktime;
    }

    public void setMarker(int marker) {
        this.marker = marker;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public void addInput(TransactionInput input) {
        inputs.add(input);
        witness.add(new WitnessProgram());
    }

    public void addOutput(TransactionOutput output) {
        outputs.add(output);
    }

    public void setWitness(int i, WitnessProgram program) {
        witness.set(i, program);
    }

    public void setWitnessPrograms(ArrayList<WitnessProgram> witness) {
        for (int i = 0; i < witness.size(); i++) {
            this.witness.set(i, witness.get(i));
        }
    }

    public void setInputScriptSigs(String[] scriptSigs) {
        for (int i = 0; i < scriptSigs.length; i++) {
            inputs.get(i).setScriptSig(scriptSigs[i]);
        }
    }

    public void clearInputs() {
        inputs.clear();
        this.clearWitness();
    }

    public ArrayList<WitnessProgram> clearWitness() {
        ArrayList<WitnessProgram> oldWitness = new ArrayList<>();
        for (WitnessProgram program : witness) {
            oldWitness.add(program);
        }
        witness.clear();
        for (int i = 0; i < inputs.size(); i++) {
            witness.add(new WitnessProgram());
        }
        return oldWitness;
    }

    public String[] clearInputScriptSigs() {
        String[] oldSigs = new String[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            oldSigs[i] = inputs.get(i).clearScriptSig();
        }
        return oldSigs;
    }

    public int getNumInputs() {
        return inputs.size();
    }

    public int getNumOutputs() {
        return outputs.size();
    }

    public ArrayList<TransactionInput> getInputs() {
        return inputs;
    }

    public ArrayList<TransactionOutput> getOutputs() {
        return outputs;
    }

    public TransactionOutput getOutputAt(int index) {
        return outputs.get(index);
    }

    public boolean isSegwit() {
        for (WitnessProgram program : witness) {
            if (program.getNumPushes() > 0) {
                return true;
            }
        }
        return false;
    }

    public String getSegwitHash(boolean littleEndian) {
        String hash = HashUtil.doubleSha256(this.getHex());
        return (littleEndian) ? ByteUtil.flipendian(hash) : hash;
    }

    public String getHash(boolean littleEndian) {
        String hash = HashUtil.doubleSha256(this.getNonSegwitHex());
        return (littleEndian) ? ByteUtil.flipendian(hash) : hash;
    }

    public String getHex() {
        if (!this.isSegwit()) {
            return this.getNonSegwitHex();
        }
        String hex = ByteUtil.int2hex(version, true);
        hex += new VarInt(marker).toHex();
        hex += new VarInt(flag).toHex();
        hex += new VarInt(inputs.size()).toHex();
        for (TransactionInput input : inputs) {
            hex += input.getHex();
        }
        hex += new VarInt(outputs.size()).toHex();
        for (TransactionOutput output : outputs) {
            hex += output.getHex();
        }
        for (WitnessProgram program : witness) {
            hex += program.getHex();
        }
        hex += ByteUtil.int2hex(locktime, true);
        return hex;
    }

    public String getNonSegwitHex() {
        String hex = ByteUtil.int2hex(version, true);
        hex += new VarInt(inputs.size()).toHex();
        for (TransactionInput input : inputs) {
            hex += input.getHex();
        }
        hex += new VarInt(outputs.size()).toHex();
        for (TransactionOutput output : outputs) {
            hex += output.getHex();
        }
        hex += ByteUtil.int2hex(locktime, true);
        return hex;
    }

    public byte[] getBytes() {
        return ByteUtil.hex2bytes(this.getHex());
    }

    public long getFee() {
        return this.getValueIn() - this.getValueOut();
    }

    public long getValueIn() {
        long valueIn = 0;
        for (int i = 0; i < inputs.size(); i++) {
            TransactionInput input = inputs.get(i);
            Outpoint outpoint = input.getOutpoint();
            Transaction refTxn = FileUtil.getTransaction(outpoint.getHash(), false);
            TransactionOutput refOutput = refTxn.getOutputAt(outpoint.getIndex());
            valueIn += refOutput.getValue();
        }
        return valueIn;
    }

    public long getValueOut() {
        long valueOut = 0;
        for (TransactionOutput output : outputs) {
            valueOut += output.getValue();
        }
        return valueOut;
    }

    public int getWeight() {
        int txWeight = (this.isSegwit() ? 42 : 40);
        for (int i = 0; i < inputs.size(); i++) {
            TransactionInput input = inputs.get(i);
            txWeight += input.getHex().length() / 2 * 4;
            txWeight += witness.get(i).getHex().length() / 2;
        }
        for (TransactionOutput output : outputs) {
            txWeight += output.getHex().length() / 2 * 4;
        }
        return txWeight;
    }

    public int getAssumedWeight() {
        int txWeight = (this.isSegwit() ? 42 : 40);
        for (int i = 0; i < inputs.size(); i++) {
            TransactionInput input = inputs.get(i);
            Outpoint outpoint = input.getOutpoint();
            Transaction txn = FileUtil.getTransaction(outpoint.getHash(), false);
            txWeight += txn.getOutputAt(outpoint.getIndex()).getAddress().getType().INPUT_WEIGHT;
        }
        for (TransactionOutput output : outputs) {
            txWeight += output.getHex().length() / 2 * 4;
        }
        return txWeight;
    }

    public static Transaction fromBytes(byte[] bytes) {
        return Transaction.fromHex(ByteUtil.hexify(bytes));
    }

    public static Transaction fromHex(String hex) {
        Transaction transaction = new Transaction();
        int offset = 0;
        transaction.setVersion(ByteUtil.hex2int(hex.substring(offset, offset += 8), true));
        VarInt numInputs = new VarInt(hex.substring(offset));
        offset += numInputs.hexLength();
        boolean segwit = (numInputs.toInt() == SEGWIT_MARKER);
        if (segwit) {
            transaction.setMarker(numInputs.toInt());
            VarInt flagVI = new VarInt(hex.substring(offset));
            offset += flagVI.hexLength();
            transaction.setFlag(flagVI.toInt());
            numInputs = new VarInt(hex.substring(offset));
            offset += numInputs.hexLength();
        }
        for (int i = 0; i < numInputs.toInt(); i++) {
            TransactionInput input = TransactionInput.fromHex(hex.substring(offset));
            transaction.addInput(input);
            offset += input.getHex().length();
        }
        VarInt numOutputs = new VarInt(hex.substring(offset));
        offset += numOutputs.hexLength();
        for (int i = 0; i < numOutputs.toInt(); i++) {
            TransactionOutput output = TransactionOutput.fromHex(hex.substring(offset));
            transaction.addOutput(output);
            offset += output.getHex().length();
        }
        if (segwit) {
            for (int i = 0; i < numInputs.toInt(); i++) {
                WitnessProgram program = WitnessProgram.fromHex(hex.substring(offset));
                offset += program.getHex().length();
                transaction.setWitness(i, program);
            }
        }
        transaction.setLocktime(ByteUtil.hex2int(hex.substring(offset, offset + 8), true));
        return transaction;
    }

    @Override
    public String toString() {
        int txWeight = (this.isSegwit() ? 42 : 40);
        boolean signed = false;
        boolean feeKnown = true;
        String res = "VERSION:  " + version + "\n";
        res += "LOCKTIME: " + locktime + "\n";
        res += "\nINPUTS:\n";
        if (inputs.isEmpty()) {
            res += "\tNONE\n\n";
        }
        for (int i = 0; i < inputs.size(); i++) {
            TransactionInput input = inputs.get(i);
            Outpoint outpoint = input.getOutpoint();
            Transaction prevTxn = FileUtil.getTransaction(outpoint.getHash(), false);
            feeKnown = feeKnown && prevTxn != null;
            if (prevTxn != null) {
                TransactionOutput output = prevTxn.getOutputAt(outpoint.getIndex());
                Address address = output.getAddress();
                res += "\tADDRESS:      ";
                if (address != null) {
                    res += address.getEncoded() + "\n";
                } else {
                    res += "UNKNOWN\n";
                }
                res += "\tAMOUNT:       " + BPSWallet.sats2btc(output.getValue()) + " BTC\n";
            }
            res += "\tPREVIOUS TXN: " + ByteUtil.flipendian(outpoint.getHash()) + "\n";
            res += "\tOUTPUT INDEX: " + outpoint.getIndex() + "\n";
            if (!input.getScriptSig().isEmpty()) {
                signed = true;
            }
            res += "\tSCRIPT:       " + input.getScriptSig() + "\n";
            res += "\tSEQUENCE:     " + ByteUtil.int2hex(input.getSequence(), true) + "\n";
            if (witness.get(i).getNumPushes() > 0) {
                signed = true;
                res += "\tWITNESS:     ";
                for (String data : witness.get(i).getPushes()) {
                    res += " " + data;
                }
                res += "\n";
                txWeight += witness.get(i).getHex().length() / 2;
            }
            res += "\n";
            txWeight += input.getHex().length() / 2 * 4;
        }
        res += "OUTPUTS:\n";
        if (outputs.isEmpty()) {
            res += "\tNONE\n\n";
        }
        for (TransactionOutput output : outputs) {
            Address address = output.getAddress();
            res += "\tADDRESS:      ";
            if (address != null) {
                res += address.getEncoded() + "\n";
            } else {
                res += "UNKNOWN\n";
            }
            res += "\tAMOUNT:       " + BPSWallet.sats2btc(output.getValue()) + " BTC\n";
            res += "\tSCRIPT:       " + output.getScriptPubKey() + "\n\n";
            txWeight += output.getHex().length() / 2 * 4;
        }
        if (feeKnown) {
            long fee = this.getFee();
            double feerate = (double) fee / Math.ceil((double) txWeight / 4);
            res += "TRANSACTION FEE:  " + BPSWallet.sats2btc(fee) + " BTC (" + String.format("%.1f", feerate) + " sat/vB)\n";
        }
        if (signed) {
            res += "HASH: " + this.getHash(true);
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof Transaction) {
            Transaction other = (Transaction) o;
            return this.getHash(false).equalsIgnoreCase(other.getHash(false));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getHash(false));
    }
}

package memory;

public interface MemoryUnit {
    public int read(int address);
    public boolean write(int address, byte value);
}

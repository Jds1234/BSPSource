/*
** 2011 April 5
**
** The author disclaims copyright to this source code.  In place of
** a legal notice, here is a blessing:
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
*/

package info.ata4.bsplib.lump;

import info.ata4.bsplib.compression.LzmaBuffer;
import info.ata4.bsplib.io.ByteBufferInputStream;
import info.ata4.bsplib.io.ByteBufferOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic lump class for the normal lump and the game lump.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public abstract class AbstractLump {
    
    private static final Logger L = Logger.getLogger(AbstractLump.class.getName());
    
    private ByteBuffer buffer = ByteBuffer.allocate(0);
    private int offset;
    private int version = 0;
    private int fourCC = 0;
    private boolean compressed = false;

    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Returns the number of bytes present in this lump. If the lump is
     * compressed, the uncompressed size will be returned.
     * 
     * @return lump length
     */
    public int getLength() {
        return buffer.limit();
    }

    /**
     * Returns the buffer for this lump. The returned buffer is usually
     * read-only, use {@link getDataOutput()} or {@link getOutputStream()} for
     * write access.
     * 
     * @return byte buffer of this lump
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }
    
    public void setBuffer(ByteBuffer buf, int offset, int length) {
        // create sub-buffer from parent buffer
        int oldPos = buf.position();
        buf.position(offset);
        ByteBuffer childBuffer = buf.slice();
        childBuffer.limit(length);
        childBuffer.order(buf.order());
        buf.position(oldPos);

        setBuffer(childBuffer);
        
        this.offset = offset;
    }
    
    public void setBuffer(ByteBuffer buf) {
        buffer = buf;
        buffer.rewind();
        setCompressed(LzmaBuffer.isCompressed(buf));
    }
    
    public InputStream getInputStream() {
        return new ByteBufferInputStream(getBuffer(), true);
    }
    
    public OutputStream getOutputStream(int newCapacity) {
        checkWriteBuffer(newCapacity);
        return new ByteBufferOutputStream(getBuffer(), true);
    }
    
    public OutputStream getOutputStream() {
        return getOutputStream(getBuffer().limit());
    }

    public LumpDataInput getDataInput() {
        return new LumpDataInput(this);
    }
    
    public LumpDataOutput getDataOutput(int newCapacity) {
        checkWriteBuffer(newCapacity);
        return new LumpDataOutput(this);
    }
    
    public LumpDataOutput getDataOutput() {
        return getDataOutput(getBuffer().limit());
    }
    
    public void setVersion(int vers) {
        this.version = vers;
    }

    public int getVersion() {
        return version;
    }

    public int getFourCC() {
        return fourCC;
    }

    public void setFourCC(int fourCC) {
        this.fourCC = fourCC;
    }

    public boolean isCompressed() {
        return compressed;
    }
    
    public void compress() {
        if (compressed) {
            return;
        }
        
        try {
            buffer = LzmaBuffer.compress(buffer);
        } catch (IOException ex) {
            L.log(Level.SEVERE, "Couldn't compress lump " + this, ex);
        }
        
        setCompressed(true);
    }
    
    public void uncompress() {
        if (!compressed) {
            return;
        }
        
        try {
            buffer = LzmaBuffer.uncompress(buffer);
        } catch (IOException ex) {
            L.log(Level.SEVERE, "Couldn't uncompress lump " + this, ex);
        }
        
        setCompressed(false);
    }
    
    protected void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }
    
    /**
     * Re-allocates the buffer if the current one is read-only or with a 
     * different capacity than given. All data in the current lump buffer will
     * be lost. If the lump was compressed before, it will be uncompressed
     * afterwards.
     * 
     * @param newCapacity desired new capacity
     */
    protected void checkWriteBuffer(int newCapacity) {
        if (buffer.isReadOnly() || buffer.capacity() != newCapacity) {
            ByteOrder bo = buffer.order();
            buffer = ByteBuffer.allocateDirect(newCapacity).order(bo);
            setCompressed(false);
        }
    }

    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }
}

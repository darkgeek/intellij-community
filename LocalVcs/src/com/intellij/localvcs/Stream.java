package com.intellij.localvcs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Stream {
  private DataInputStream myIs;
  private DataOutputStream myOs;
  private Storage myStorage;

  public Stream(InputStream is, Storage s) {
    myStorage = s;
    myIs = new DataInputStream(is);
  }

  public Stream(OutputStream os) {
    myOs = new DataOutputStream(os);
  }

  public Storage getStorage() {
    return myStorage;
  }

  public IdPath readIdPath() throws IOException {
    return new IdPath(this);
  }

  public void writeIdPath(IdPath p) throws IOException {
    p.write(this);
  }

  public Entry readEntry() throws IOException {
    return (Entry)readInstanceOf(myIs.readUTF());
  }

  public void writeEntry(Entry e) throws IOException {
    myOs.writeUTF(e.getClass().getName());
    e.write(this);
  }

  public Change readChange() throws IOException {
    return (Change)readInstanceOf(myIs.readUTF());
  }

  public void writeChange(Change c) throws IOException {
    myOs.writeUTF(c.getClass().getName());
    c.write(this);
  }

  public ChangeSet readChangeSet() throws IOException {
    return new ChangeSet(this);
  }

  public void writeChangeSet(ChangeSet c) throws IOException {
    c.write(this);
  }

  public ChangeList readChangeList() throws IOException {
    return new ChangeList(this);
  }

  public void writeChangeList(ChangeList c) throws IOException {
    c.write(this);
  }

  public String readString() throws IOException {
    // todo remove null-saving after refactoring RootEntry 
    if (!myIs.readBoolean()) return null;
    return myIs.readUTF();
  }

  public void writeString(String s) throws IOException {
    // todo make it not-nullable
    // todo writeUTF is very time consuming
    myOs.writeBoolean(s != null);
    if (s != null) myOs.writeUTF(s);
  }

  public Integer readInteger() throws IOException {
    if (!myIs.readBoolean()) return null;
    return myIs.readInt();
  }

  public void writeInteger(Integer i) throws IOException {
    myOs.writeBoolean(i != null);
    if (i != null) myOs.writeInt(i);
  }

  public Long readLong() throws IOException {
    if (!myIs.readBoolean()) return null;
    return myIs.readLong();
  }

  public void writeLong(Long l) throws IOException {
    myOs.writeBoolean(l != null);
    if (l != null) myOs.writeLong(l);
  }

  public Content readContent() throws IOException {
    if (!myIs.readBoolean()) return null;
    return new Content(this);
  }

  public void writeContent(Content c) throws IOException {
    myOs.writeBoolean(c != null);
    if (c != null) c.write(this);
  }

  private Object readInstanceOf(String className) throws IOException {
    try {
      Class clazz = Class.forName(className);
      Constructor constructor = clazz.getConstructor(getClass());
      return constructor.newInstance(this);
    } catch (InvocationTargetException e) {
      throw (IOException)e.getCause();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

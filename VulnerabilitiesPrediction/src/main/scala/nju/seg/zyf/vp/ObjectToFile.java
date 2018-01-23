package nju.seg.zyf.vp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

/**
 * 用于将一个可序列化对象与一个文件关联，即可以将可序列化对象写入文件、从文件读出。
 * 可以扩展此类，在序列化等操作时附带一些额外的操作。
 *
 * @author Zhang Yifan
 */
@SuppressWarnings("ALL")
@ParametersAreNonnullByDefault
@Beta
public final class ObjectToFile<T extends Serializable> {

  private final AtomicBoolean _IsWritten = new AtomicBoolean(false);

  private final File _File;

  private ObjectToFile(final File e) throws IOException {
    // assert e != null;

    //    if (!e.exists()) {
    //      e.createNewFile();
    //    }

    this._File = e;
  }

  /**
   * @param canOverwrite 指示是否能在已经用 writeObject 写入一次对象后，覆写之前写入的对象。
   */
  public void WriteObject(final T obj, final boolean canOverwrite) throws IOException {
    Preconditions.checkNotNull(obj);
    Preconditions.checkArgument(this._File.canWrite(), "Can't write to file.");
    if (!canOverwrite) {
      Preconditions.checkArgument(this._IsWritten.compareAndSet(false, true), "Can't overwrite.");
    } else {
      this._IsWritten.set(true);
    }

    try (final FileOutputStream fo = new FileOutputStream(this._File, false);
         final ObjectOutputStream oo = new ObjectOutputStream(fo)) {
      oo.writeObject(obj);
      oo.flush();
    }
  }

  public void WriteObject(final T obj) throws IOException {
    Preconditions.checkNotNull(obj);

    this.WriteObject(obj, true);
  }

  public boolean TryWriteObject(final T obj, final boolean canOverwrite) {
    Preconditions.checkNotNull(obj);

    try {
      this.WriteObject(obj, canOverwrite);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  public boolean TryWriteObject(final T obj) {
    Preconditions.checkNotNull(obj);

    return this.TryWriteObject(obj, true);
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  public T ReadObject() throws IOException, ClassNotFoundException {
    try (final FileInputStream fi = new FileInputStream(this._File);
         final ObjectInputStream oi = new ObjectInputStream(fi)) {
      return (T) oi.readObject();
    }
  }

  public Optional<T> TryReadObject() {
    try {
      final T res = this.ReadObject();
      return Optional.of(res);
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  @Nonnull
  @CheckReturnValue
  public static <T extends Serializable> ObjectToFile<T> Create(final File file) throws IOException {
    Preconditions.checkNotNull(file);

    return new ObjectToFile<>(file);
  }
}

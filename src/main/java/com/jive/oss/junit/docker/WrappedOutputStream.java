package com.jive.oss.junit.docker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class WrappedOutputStream extends OutputStream
{

  private final PrintStream out;
  private int counter;

  public WrappedOutputStream(final OutputStream out)
  {
    this.out = new PrintStream(out);
    this.counter = 0;
  }

  @Override
  public void write(final int b) throws IOException
  {

    if (this.counter++ == 0)
    {
      this.out.print("DOCKER: ");
    }

    if (b == '\n')
    {
      this.counter = 0;
    }

    this.out.write(b);

  }

  @Override
  public void flush() throws IOException
  {
    this.out.flush();
  }

  @Override
  public void close() throws IOException
  {
    this.out.close();
  }

}

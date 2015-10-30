package com.jive.oss.junit.docker;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExtRunner extends BlockJUnit4ClassRunner
{

  private final Class<?> klass;

  public ExtRunner(final Class<?> klass) throws InitializationError
  {
    super(klass);
    this.klass = klass;
  }

  @Override
  public void run(final RunNotifier notifier)
  {

    final RunIf rif = this.getRif();

    if (rif != null)
    {

      Predicate<Object> test;

      try
      {
        test = rif.value().newInstance();
      }
      catch (InstantiationException e)
      {
        throw new RuntimeException(e);
      }
      catch (IllegalAccessException e)
      {
        throw new RuntimeException(e);
      }

      if (!test.test(notifier))
      {
        log.info("Skipping test");
        notifier.fireTestIgnored(getDescription());
        return;
      }

    }

    super.run(notifier);

  }

  private RunIf getRif()
  {
    for (final Annotation a : this.getRunnerAnnotations())
    {
      if (a instanceof RunIf)
      {
        return (RunIf) a;
      }
    }
    return null;
  }

}

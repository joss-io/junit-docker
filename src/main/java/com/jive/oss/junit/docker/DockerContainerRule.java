package com.jive.oss.junit.docker;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.common.base.Joiner;
import com.google.common.net.HostAndPort;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient.ExecParameter;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ExecState;
import com.spotify.docker.client.messages.HostConfig;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

/**
 * Starts up the specified docker image, the handle can be used for executing commands in the image.
 *
 * The image will be torn down when the test finishes.
 *
 */

@Slf4j
public class DockerContainerRule extends ExternalResource
{

  public static final class Available implements Predicate<Object>
  {

    public boolean test(final Object t)
    {
      if (System.getenv().containsKey("DOCKER_CERT_PATH"))
      {
        return true;
      }
      else if (System.getProperties().containsKey("judocker.certpath"))
      {
        return true;
      }
      log.info("No DOCKER_CERT_PATH env or -Djudocker.certpath, skipping docker tests.");
      return false;
    }

  }

  private final String image;
  private DefaultDockerClient docker;
  private ContainerCreation container;
  private final Set<String> expose = Sets.newHashSet();
  private final List<String> binds = Lists.newLinkedList();
  private ContainerInfo info;
  private Boolean privileged  =false;

  public DockerContainerRule(final String image)
  {
    this.image = image;
  }

  public DockerContainerRule expose(final String port)
  {
    this.expose.add(port);
    return this;
  }
  
  public DockerContainerRule bind(final String volume)
  {
    this.binds.add(volume);
    return this;
  }
  
  public DockerContainerRule privileged(final boolean privileged)
  {
    this.privileged = privileged;
    return this;
  }

  @Override
  public Statement apply(final Statement base, final Description description)
  {
    return super.apply(base, description);
  }

  @Override
  protected void before() throws Throwable
  {

    super.before();

    if (System.getenv().containsKey("DOCKER_CERT_PATH"))
    {
      this.docker = DefaultDockerClient.builder()
          .uri(System.getenv("DOCKER_HOST").replaceAll("^tcp", "https"))
          .dockerCertificates(DockerCertificates.builder().dockerCertPath(Paths.get(System.getenv("DOCKER_CERT_PATH"))).build())
          .build();
    }
    else if (System.getProperties().containsKey("judocker.certpath"))
    {

      this.docker = DefaultDockerClient.builder()
          .uri(System.getProperty("judocker.host").replaceAll("^tcp", "https"))
          .dockerCertificates(DockerCertificates.builder().dockerCertPath(Paths.get(System.getProperty("judock.certpath"))).build())
          .build();
    }
    else
    {
      throw new RuntimeException(String.format("Can't find junit docker configuration properties.  Set DOCKER_* envs, or -Djudocker.certpath and -Djudocker.host"));
    }

    this.docker.ping();

    final HostConfig hostConfig = HostConfig.builder()
        .privileged(this.privileged)
        .binds(this.binds)
        .publishAllPorts(true)
        .build();

    final ContainerConfig containerConfig = ContainerConfig.builder()
        .image(this.image)
        .exposedPorts(this.expose)
        .hostConfig(hostConfig)
        .build();

    this.container = this.docker.createContainer(containerConfig);

    log.info("Created docker container: {}", this.container);

    this.docker.startContainer(this.container.id());

    int tries = 0;

    while (tries++ < 600)
    {
      this.info = this.docker.inspectContainer(this.container.id());

      if (!this.info.state().running())
      {
        Thread.sleep(100);
        continue;
      }

      return;

    }

    this.docker.removeContainer(this.container.id(), true);

    throw new RuntimeException("Failed to start docker image after 60 seconds");

  }

  @Override
  protected void after()
  {
    super.after();
    try
    {
      log.info("Shutting down docker container {}", this.container.id());
      this.docker.killContainer(this.container.id());
      this.docker.removeContainer(this.container.id(), true);
    }
    catch (final DockerException e)
    {
      throw new RuntimeException(e);
    }
    catch (final InterruptedException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * 
   */

  public HostAndPort target(final String port)
  {
    return HostAndPort.fromParts(this.docker.getHost(), Integer.parseInt(this.info.networkSettings().ports().get(port).get(0).hostPort()));
  }

  /**
   * Execute the given command in the docker container;
   */

  public String execute(final String... cmds)
  {

    log.info("Executing in container: {}", Joiner.on(' ').join(cmds));
    try
    {

      final String eid = this.docker.execCreate(this.container.id(), cmds, ExecParameter.STDERR, ExecParameter.STDOUT);
      final LogStream strm = this.docker.execStart(eid);

      strm.attach(new WrappedOutputStream(System.out), new WrappedOutputStream(System.out));

      final String out = strm.readFully();
      final ExecState exit = this.docker.execInspect(eid);

      if (exit.exitCode() != 0)
      {
        throw new RuntimeException(out.trim());
      }

      return out;

    }
    catch (final DockerException  e)
    {
      throw new RuntimeException(e);
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
    catch (final InterruptedException e)
    {
      throw new RuntimeException(e);
    }
  }

}
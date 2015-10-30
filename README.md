# JUnit Docker Integration

Makes it easy to use docker to run some services for testing purposes.

## Maven

```
<dependency>
	<groupId>com.jive.oss.junit.docker</groupId>
	<artifactId>junit-docker</artifactId>
	<version>{version}</version>
	<scope>test</scope>
</dependency>
```

## Usage

Uses DOCKER_CERTPATH and DOCKER_HOST environment variables or -Djudocker.certpath and -Djudocker.host.

To use, create a public variable annotated with @Rule, defining the container you want set up:

```
  @Rule
  public final DockerContainerRule docker = new DockerContainerRule("gcr.io/something:1.2.3").expose("8080/tcp");
```

### Only running if docker is available.

If you want your tests to be skipped if the local environment isn't set up, then add this to your test:

  @RunWith(ExtRunner.class)
  @RunIf(DockerContainerRule.Available.class)


### Example

```

import com.jive.oss.junit.docker.DockerContainerRule;
import com.jive.oss.junit.docker.ExtRunner;
import com.jive.oss.junit.docker.RunIf;


@RunIf(DockerContainerRule.Available.class)
@RunWith(ExtRunner.class)
public class MyTest
{

  @Rule
  public final DockerContainerRule docker = new DockerContainerRule("gcr.io/something:1.2.3").expose("8080/tcp");

  /**
   */

  @Test
  public void testClient() throws InterruptedException
  {
  
    final MyClient client = new MyClient(this.docker.target("6640/tcp"));

    client.connect();

    // check the state
    Assert.assertEquals(ImmutableSet.of(), client.somerthing());

    // make a modification in the container
    this.docker.execute("somecmd", "arg");

    // now make another test
    Assert.assertEquals(ImmutableSet.of("jovstest"), client.something());

	// close
    client.close();

	// do whatever you want with this.docker here.

  }

}
```


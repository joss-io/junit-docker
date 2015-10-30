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

To use, create a public variable annotated with @Rule, defining the container you want set up.  For example, to use an image named 
*gcr.io/something:1.2.* and expose the port *8080/tcp* to be accessible to the runtime:

```
  @Rule
  public final DockerContainerRule docker = new DockerContainerRule("gcr.io/something:1.2.3").expose("8080/tcp");
```


### Only running if docker is available.

If you want your tests to be skipped if the local environment isn't set up, you can use the helper runner:

```
  @RunWith(ExtRunner.class)
  @RunIf(DockerContainerRule.Available.class)
```
  
you can of course use any other runner to skip the tests if you prefer.
  

## Example

A more in-depth example

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
  
    final MyClient client = new MyClient(this.docker.target("8080/tcp"));

    // note: this might take a while, as the container has to start it's process and may not be ready immediately.
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


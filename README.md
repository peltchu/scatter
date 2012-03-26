## Whaat?

**Scatter** is a Scala library for testing web services written with the
awesome [Spray](http://spray.cc/) framework. Although Spray already supports testing,
Scatter aims to further simplify and enhance the task.
Scatter achieves this by providing a powerful and clean way for building and
maintaining service tests.

Scatter is currently targeted at Scala 2.9.1 and Spray 0.9.0. It can be used both
with ScalaTest and specs2.

Ideas and contributions are welcome!

## Installation

As there's no release yet only snapshots are currently available.
If you're using [SBT](https://github.com/harrah/xsbt/), it's enough to add the
repository and dependencies to your *build.sbt*:


    resolvers += "scatter-repo" at "http://vortex.fi/repo/snapshots/"

	libraryDependencies += "org.scatter" %% "scatter" % "0.1-SNAPSHOT" % "test"


## How It Works

Scatter's design is simple yet quite effective. **Every Scatter test
is built in same manner**:

1. The test request is built by combining any number of *request transformations*
2. The request is passed to the appropriate Spray route, either implicitly or explicitly
3. The received response is validated against a combination of *response validations*
    * Upon validation, different validation failures (if any) are collected together and then displayed as a failure.

Request transformations can be viewed as fragments of a complete request which,
when combined together, make the full request. Response validations, on the other hand,
are self-contained but you can chain as many of them together as you want to.

**What about test maintainability?** Well, first of all,
the way tests are written helps keeping the test code clean and tidy as the pattern is always
the same and much of the test boilerplate code remains hidden. Secondly, as you can combine arbitrary number of
transformations and validations, you can build your own project-specific "libraries"
out of them and then combine those again in new ways. This promotes reuse and reduces test code duplication.


## Okay, Let's Roll!

An example probably tells more than thousand words so let's come up with one.
Let's say that you are making a dog registry service. The users of the service
 can add and remove dogs from the service and get information about a specified dog.
  Thus, you could write tests such as these with Scatter (+ ScalaTest):

	import org.scalatest._
	import cc.spray.test._
	import org.scatter.Scatter

	import DogJsonProtocol._

	class TestSuite extends FunSpec with SprayTest with Scatter {

		// 1
		implicit val route = new DogService().route

		implicit val rejectionHandler = dogServiceRejectionHandler orElse RejectionHandler.Default

		describe("Dog Service") {

			// 2
            it("returns dog information as json when asking dog information") {
                GET("dogs/Vladimir") >>> route ==> hasExactJson(Dog("Vladimir", 2))
            }

			// 3
            it("returns 404 when requesting information about non-existent dog") {
                GET("dogs/Nemo") ==> hasStatus(404)
            }

			// 4
            it("returns the dog's information as json after successfully adding a new dog to service") {
                POST("dogs") ~ json(Dog("Decimus", 10)) ==>
                    hasJson[Dog](_.name == "Decimus") ~ hasStatus(201)
            }

			// 5
            it("returns an empty response with status 204 after successfully removing a dog from the service") {
                val response = DELETE("dogs/Fido") >>> route
                response ==> hasStatus(204) ~ hasNoContent
            }
        }
	}

Now, let's break this small example to pieces.

First thing you'll probably notice is that we're not mixing in the service to the test
but initializing it as an attribute instead (1). This is because Scatter has naming
collisions with the Spray's route DSL (there are only so many good keywords :)

First test case is  (2). The request is built (`GET("dogs/Vladimir")`), then
it is passed to route (`>>> route`) and finally the service result is validated
with a single validation (`hasExactJson(Dog("Vladimir", 2)`) which makes sure that the result
is exactly a json representation of a dog with name "Vladimir" and age 2 (notice that we are
using custom json protocol for doing the unmarshalling, see [spray-json](https://github.com/spray/spray-json)
for that).

In (3) there's a test case that resembles much the first but this time there's no explicit
passing of the route. Instead, the route is picked up implicitly as we've defined it as implicit
in (1). This is useful if you are testing the same route in all your cases in the scope.

In (4) you can see both *request transformation* and *response validation*
chaining (combining) by using "~".

In the last example (5), the test is broken into two pieces. The response is actually Spray
`HttpResponse` so if you want, you can do your hand-made validations with it as well. Naturally,
you can also extend the existing validators to make your own (see [apidoc](http://peltchu.github.com/scatter/api/)). Additionally, you
can also build the Spray's `HttpRequest` by hand and then just use the validators
to validate the response:

    HttpRequest(HttpMethods.GET, "/foo") ==> hasStatus(200)


One thing to do is make your own combinations and then reuse them, something like this:

    val baseReq = path("dogs/") ~ charset(`UTF-8`)
    val basicSuccess = hasStatus(200) ~ hasCharset(`UTF-8`)

    baseReq ~ GET("Vladimir") ==> basicSuccess ~ hasJson(Dog("Vladimir", 2))
    baseReq ~ POST ~ json(dog) ==> hasStatus(201)
    // etc...


## Request Transformations

So what request transformations are currently available for building the request?



<table>
  <tr>
    <th>Transformation</th><th>Description</th><th>Examples</th>
  </tr>
  <tr>
    <td>GET</td><td>sets the request method to GET and optionally adds a path fragment</td><td>GET, GET("foo")</td>
  </tr>
  <tr>
    <td>POST</td><td> </td><td> </td>
  </tr>
  <tr>
    <td>PUT</td><td> </td><td> </td>
  </tr>
  <tr>
    <td>DELETE</td><td> </td><td> </td>
  </tr>
  <tr>
    <td>HEAD</td><td> </td><td> </td>
  </tr>
  <tr>
    <td>OPTIONS</td><td> </td><td> </td>
  </tr>
   <tr>
    <td>TRACE</td><td> </td><td> </td>
  </tr>
  <tr>
    <td>path</td><td>adds a path fragment to request</td><td>path("foo")</td>
  </tr>
  <tr>
    <td>header</td><td>adds header to request</td><td>header("Cache-Control", "no-cache"), header(`Accept`(`text/plain`)</td>
  </tr>
  <tr>
    <td>charset</td><td>sets the charset of request</td><td>charset(`UTF-8`)</td>
  </tr>
  <tr>
    <td>form</td><td>sets the request content to be form with given fields</td><td>form("foo" -> "bar")</td>
  </tr>
  <tr>
    <td>json</td><td>sets the request content to be given data as json</td><td>json(Map("foo" -> "bar"))</td>
  </tr>
</table>

Refer to the [apidoc](http://peltchu.github.com/scatter/api/) for further information.

## Response Validations

Here's a list of available response validations. Again, refer to the [apidoc](http://peltchu.github.com/scatter/api/) for more info.

<table>
  <tr>
    <th>Validation</th><th>Description</th><th>Examples</th>
  </tr>
  <tr>
    <td>hasStatus</td><td>validates that request has given status</td><td>hasStatus(200), hasStatus(OK)</td>
  </tr>
  <tr>
    <td>hasHeader</td><td>validates that response has given header</td><td>hasHeader("Cache-Control", "no-cache"), hasHeader("Cache-Control", "no.*".r)</td>
  </tr>
  <tr>
    <td>hasMediaType</td><td>validates that response has given media type</td><td>hasMediaType(`text/plain`)</td>
  </tr>
  <tr>
    <td>hasCharset</td><td>validates that response has given charset</td><td>hasCharset(`UTF-8`)</td>
  </tr>
  <tr>
    <td>hasNoContent</td><td>validates that response has no content</td><td> </td>
  </tr>
  <tr>
    <td>hasText</td><td>validates that the content (as text) matches the given string</td><td>hasText("foobar"), hasText("foob?r".r)</td>
  </tr>
  <tr>
    <td>hasExactJson</td><td>validates that content is json with exactly same data as given</td><td>hasExactJson(Map("foo" -> "bar"))</td>
  </tr>
  <tr>
    <td>hasJson</td><td>validates that content is json and fulfills the given predicate</td><td>hasJson[List[String]](_.size == 5)</td>
  </tr>
  <tr>
    <td>_not</td><td>negates the validation</td><td>_not(hasStatus(200))</td>
  </tr>
  <tr>
    <td>oneOf</td><td>validates that only one of the given validations is true</td><td>oneOf(hasStatus(200), hasStatus(201))</td>
  </tr>
 </table>

## License

The library is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)


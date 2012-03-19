/*
 * Copyright 2012 @peltchu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scatter

import cc.spray.test.SprayTest
import cc.spray.{Directives, Route}
import org.scalatest.matchers.MustMatchers
import cc.spray.http.StatusCodes._
import cc.spray.http.{HttpHeader, HttpResponse}
import org.scalatest.{TestFailedException, FunSuite}


class PlumbingSuite extends FunSuite with
                            MustMatchers with SprayTest with Plumbing
                            with Transformations with Validations {

	object TestService extends Directives {
		val route: Route = {
			path("foo") {
				get {
					_.complete(Created, "foo")
				}
			}
		}
	}

	test("service request") {
		val requestChain = RequestChain(List(GET("foo")))
		val serviceRequest = new ServiceRequest(requestChain.toHttpRequest)
		val response = serviceRequest >>> TestService.route
		response.status must be(Created)
	}

	test("validator (success)") {
		val response = HttpResponse(OK, List(HttpHeader("Cache-Control", "no-cache")))
		new Validator(response) ==> hasStatus(OK) ~ hasHeader("Cache-Control", "no-cache")
		new Validator(response) validateWith hasStatus(OK) ~ hasHeader("Cache-Control", "no-cache")
	}

	test("validator (failure)") {
		val response = HttpResponse(OK, List(HttpHeader("Cache-Control", "no-cache")))
		intercept[TestFailedException] {
			new Validator(response) ==> hasStatus(OK) ~ hasHeader("Cache-Control", "public")
		}
	}

	test("validator (validation exception)") {
		import cc.spray.typeconversion._
		import cc.spray.typeconversion.DefaultMarshallers._
		import cc.spray.typeconversion.SprayJsonSupport._
		import cc.spray.json.DefaultJsonProtocol._
		val response = HttpResponse(OK, "foobar".toHttpContent)
		intercept[TestFailedException] {
			new Validator(response) ==> hasExactJson(List("fubaz"))
		}
	}

}

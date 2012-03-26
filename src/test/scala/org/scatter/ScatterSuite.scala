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

import cc.spray._
import cc.spray.test.SprayTest
import cc.spray.http._
import cc.spray.http.StatusCodes._
import org.scalatest.{TestFailedException, FunSuite}
import cc.spray.{AuthenticationFailedRejection, Directives}

class ScatterSuite extends FunSuite with SprayTest with Scatter {
	object TestService extends Directives {
		val route = {
			path("foo") {
				get {
					_.complete("FOO")
				}
			}
		}
		val route2 = {
			path("bar") {
				post {
					_.complete("BAR")
				}
			}
		}
	}

	test("explicit route") {
		GET("foo") >>> TestService.route ==> hasStatus(200) ~ hasText("FOO")
    intercept[TestFailedException] {
      GET("foo") >>> TestService.route ==> hasStatus(200) ~ hasText("boo")
    }
	}

	test("implicit route") {
		implicit val route = TestService.route
		GET("foo") ==> hasStatus(200) ~ hasText("FOO")
	}

	test("mixed route") {
		implicit val route = TestService.route
		POST("bar") >>> TestService.route2 ==> hasStatus(200) ~ hasText("BAR")
	}

  test("HttpRequest conversions") {
    val req = HttpRequest(HttpMethods.GET, "/foo")

    req >>> TestService.route ==> hasStatus(200)

    implicit val route = TestService.route
    req ==> hasStatus(200)
  }

	test("custom rejection handler") {
		implicit val customRejectionHandler: RejectionHandler = {
			case AuthenticationFailedRejection(realm) :: _ =>
				HttpResponse(Unauthorized, "You shall not pass!")
		}

		implicit val testRoute: Route = {
			_.reject(AuthenticationFailedRejection("foo"))
		}

		GET ==> hasStatus(Unauthorized) ~ hasText("You shall not pass!")
	}
}

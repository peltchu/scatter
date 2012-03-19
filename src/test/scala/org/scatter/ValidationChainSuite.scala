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

import org.scalatest.FunSuite
import org.scalatest.matchers.MustMatchers
import cc.spray.http.HttpResponse

class ValidationChainSuite extends FunSuite with MustMatchers {
	
	class DummyValidation(val result:Boolean) extends ResponseValidation {
		def validate(response: HttpResponse) = {
			ValidationResult(result, "foo" -> "bar")
		}
	}
	
	test("validation") {
		val v = List(new DummyValidation(true),
			new DummyValidation(false),
			new DummyValidation(true))
		
		val failures = ValidationChain(v).validate(HttpResponse(200))

		failures.size must be (1)
		failures(0).success must be (false)
	}
}

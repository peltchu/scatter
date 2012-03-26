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

import cc.spray.http.{HttpResponse, HttpRequest}
import cc.spray.test.SprayTest
import cc.spray.{HttpServiceLogic, RejectionHandler}


private[scatter] trait Plumbing {
	this: SprayTest =>

	class Validator(response: HttpResponse) {

		def ==>(validationChain: ValidationChain) = {
			validateWith(validationChain)
		}

		def validateWith(validationChain: ValidationChain): Boolean = {
			try {
				val failures = validationChain.validate(response)
				if (failures.isEmpty)
					return true

				signalFailure(failStr(failures))

			} catch {
				case e: ValidationException => signalFailure("Validation failed: " + e.getMessage)
			}

			false
		}

		private def failStr(failures: List[ValidationResult]): String = {
			failures.map(_.describe).mkString("Validations failed: ", ", ", "")
		}


	}


	class ServiceRequest(val request: HttpRequest) {

		def >>>[A](route: A)
		          (implicit c: A => HttpServiceLogic,
		           rejectionHandler: RejectionHandler = RejectionHandler.Default): HttpResponse = {
			testService(request)(route).response
		}
	}


	/**
	 * copied from SprayTest (spray 0.8.0)
	 * @see cc.spray.test.SprayTest#doFail(String)
	 */
	protected def signalFailure(reason: String): Nothing = {
		try {
			this.asInstanceOf[ {def fail(msg: String): Nothing}].fail(reason)
		} catch {
			case e: NoSuchMethodException => {
				try {
					this.asInstanceOf[ {def failure(msg: String): Nothing}].failure(reason)
				} catch {
					case e: NoSuchMethodException =>
						throw new RuntimeException("Illegal mixin: the Scatter trait can only be mixed into test classes that " +
							"supply a fail(String) or failure(String) method (e.g. ScalaTest, Specs or Specs2 specifications)")
				}
			}
		}
	}


}

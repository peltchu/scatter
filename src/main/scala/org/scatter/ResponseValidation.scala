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

import cc.spray.http.HttpResponse

/**
 * Describes the result of a validation
 * @param success true if validation passed
 * @param value pair consisting of validation name and expected value
 */
case class ValidationResult(success: Boolean, value: Pair[String, String]) {
	def describe = value._1 + "(" + value._2 + ")"
}

/**
 * Provides base for validations. Extend to define your own.
 */
trait ResponseValidation {
	def validate(response: HttpResponse): ValidationResult
}

object ResponseValidation {
	implicit def validation2Chain(m: ResponseValidation): ValidationChain = ValidationChain(m)
}


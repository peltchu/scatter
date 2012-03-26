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
import cc.spray.http.{HttpRequest, HttpResponse}
import cc.spray.{RejectionHandler, Route}

/**
 * Trait providing access to scatter and its request transformations and
 * response validations
 */
trait Scatter extends Plumbing with Transformations with Validations {
	this: SprayTest =>

	implicit def requestChain2Validator[A](r: A)
	                                      (implicit route: Route,
	                                       conv: A => RequestChain,
		                                     rejectionHandler:RejectionHandler = RejectionHandler.Default
		                                      ): Validator = {

		val response = new ServiceRequest(r.toHttpRequest) >>> route
		new Validator(response)
	}

	implicit def requestChain2ServiceRequest[A <% RequestChain](r: A): ServiceRequest = {
		new ServiceRequest(r.toHttpRequest)
	}

	implicit def httpRequest2Validator(r: HttpRequest)(implicit route: Route): Validator = {
		val response = new ServiceRequest(r) >>> route
		new Validator(response)
	}

	implicit def httpRequest2ServiceRequest(r: HttpRequest): ServiceRequest = {
		new ServiceRequest(r)
	}

	implicit def httpResponse2Validator(r: HttpResponse): Validator = {
		new Validator(r)
	}

}

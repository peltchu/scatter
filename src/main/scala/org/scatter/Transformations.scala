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

import cc.spray.http.HttpHeaders._
import cc.spray.http.HttpCharset
import cc.spray.http.MediaTypes._
import cc.spray.http._
import cc.spray.typeconversion._
import cc.spray.typeconversion.DefaultMarshallers._
import cc.spray.json.RootJsonWriter

trait Transformations {

	/**
	 * Adds path fragment to request
	 * @param path the path fragment
	 */
	case class path(path: String) extends RequestTransformation {
		def transform(request: HttpRequest) =
			HttpRequest(request.method, request.path + path, request.headers, request.content, request.protocol)
	}

	/**
	 * Sets request method to GET and adds path fragment to the request
	 * @param path the path fragment
	 */
	case class GET(path: String) extends MethodTransformation(HttpMethods.GET, path)

	/**
	 * Sets request method to GET
	 */
	object GET extends GET("")

	/**
	 * Sets request method to POST and adds path fragment to the request
	 * @param path the path fragment
	 */
	case class POST(path: String) extends MethodTransformation(HttpMethods.POST, path)

	/**
	 * Sets request method to POST
	 */
	object POST extends POST("")

	/**
	 * Sets request method to PUT and adds path fragment to the request
	 * @param path the path fragment
	 */
	case class PUT(path: String) extends MethodTransformation(HttpMethods.PUT, path)

	/**
	 * Sets request method to PUT
	 */
	object PUT extends PUT("")

	/**
	 * Sets request method to DELETE and adds path fragment to the request
	 * @param path the path fragment
	 */
	case class DELETE(path: String) extends MethodTransformation(HttpMethods.DELETE, path)

	/**
	 * Sets request method to DELETE
	 */
	object DELETE extends DELETE("")

	/**
	 * Sets request method to HEAD and adds path fragment to the request
	 * @param path the path fragment
	 */
	case class HEAD(path: String) extends MethodTransformation(HttpMethods.HEAD, path)

	/**
	 * Sets request method to HEAD
	 */
	object HEAD extends HEAD("")

	/**
	 * Sets request method to OPTIONS and adds path fragment to the request
	 * @param path the path fragment
	 */
	case class OPTIONS(path: String) extends MethodTransformation(HttpMethods.OPTIONS, path)

	/**
	 * Sets request method to OPTIONS
	 */
	object OPTIONS extends OPTIONS("")

	/**
	 * Sets request method to TRACE and adds path fragment to the request
	 * @param path the path fragment
	 */
	case class TRACE(path: String) extends MethodTransformation(HttpMethods.TRACE, path)

	/**
	 * Sets request method to TRACE
	 */
	object TRACE extends TRACE("")

	/**
	 * Adds header to the request
	 *
	 * Also see the companion object for other methods of creation
	 * @param name header name
	 * @param value header value
	 */
	case class header(name: String, value: String) extends RequestTransformation {
		def transform(request: HttpRequest) = {
			val hdrs = HttpHeader(name, value) :: request.headers
			HttpRequest(request.method, request.path, hdrs, request.content, request.protocol)
		}
	}

	/**
	 * Adds alternative methods for request header creation
	 */
	object header {
		/**
		 * Adds header to the request
		 * @param hdr the header
		 * @return
		 */
		def apply(hdr: HttpHeader): header = {
			new header(hdr.name, hdr.value)
		}
	}

	/**
	 * Sets the accepted charset of the request
	 * @param cset the charset
	 */
	case class charset(cset: HttpCharset) extends RequestTransformation {
		def transform(request: HttpRequest) = {
			// create dummy content for passing on the desired charset in the chain
			val dummyContent = request.content match {
				case Some(x) => throw new UnsupportedOperationException(
					"Changing charset of existing content is not currently supported " +
						"(charset must be defined before setting content in the chain)")
				case None => Some(HttpContent(ContentType(`text/plain`, cset), ""))
			}
			HttpRequest(request.method, request.path, request.headers, dummyContent, request.protocol)
		}
	}

	/**
	 * Sets the content of the request to be json with the given data
	 * @param data the data
	 * @tparam A the type of data (to be marshalled to json)
	 */
	case class json[A: RootJsonWriter](data: A)
		extends ContentTransformation[A](data, `application/json`) {
		def transformContent(data: A, contentType: ContentType) =
			Some(SprayJsonSupport.sprayJsonMarshaller[A].marshal(data, contentType))
	}

	/**
	 * Sets the content of the request to be a form with the given fields
	 * @param data the fields
	 */
	case class form(data: Pair[String, String]*)
		extends ContentTransformation[Seq[Pair[String, String]]](data, `application/x-www-form-urlencoded`) {
		def transformContent(data: Seq[(String, String)], contentType: ContentType) = {
			Some(DefaultMarshallers.FormDataMarshaller.marshal(FormData(data.toMap), contentType))
		}
	}

	/**
	 * Helper superclass for content transformation
	 * @param d content data
	 * @param mType media type of the content
	 * @tparam A type of data
	 */
	private[scatter] abstract class ContentTransformation[A](val d: A, val mType: MediaType) extends RequestTransformation {
		def transform(request: HttpRequest) = {
			val contentType = request.content match {
				// honor existing charset, if present in the chain (see "charset" transformation)
				case Some(c) => ContentType(mType, c.contentType.charset)
				case None => ContentType(mType)
			}
			val content = transformContent(d, contentType)
			HttpRequest(request.method, request.path, request.headers, content, request.protocol)
		}

		def transformContent(data: A, contentType: ContentType): Option[HttpContent]
	}

	/**
	 * Helper superclass for method transformations
	 * @param method the method
	 * @param u the path fragment
	 */
	private[scatter] abstract class MethodTransformation(val method: HttpMethod, val u: String)
		extends RequestTransformation {
		def transform(request: HttpRequest) =
			HttpRequest(
				method,
				request.path + u,
				request.headers,
				request.content,
				request.protocol)
	}

}

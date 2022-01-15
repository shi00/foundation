package com.silong.foundation.constants;

/**
 * http status code 常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 12:32
 */
public interface HttpStatusCode {

  /**
   * 100 Continue
   */
  String CONTINUE = "100";

  /**
   * 101 Switching Protocols
   */
  String SWITCHING_PROTOCOLS = "101";

  /**
   * 102 Processing
   */
  String PROCESSING = "102";

  //  --- 2xx Success ---

  /**
   * 200 OK
   */
  String OK = "200";

  /**
   * 201 Created
   */
  String CREATED = "201";

  /**
   * 202 Accepted
   */
  String ACCEPTED = "202";

  /**
   * 203 Non Authoritative Information
   */
  String NON_AUTHORITATIVE_INFORMATION = "203";

  /**
   * 204 No Content
   */
  String NO_CONTENT = "204";

  /**
   * 205 Reset Content
   */
  String RESET_CONTENT = "205";

  /**
   * 206 Partial Content
   */
  String PARTIAL_CONTENT = "206";

  /**
   * 207 Multi-Status or 207 Partial Update OK
   */
  String MULTI_STATUS = "207";

  //  --- 3xx Redirection ---

  /**
   * 300 Mutliple Choices
   */
  String MULTIPLE_CHOICES = "300";

  /**
   * 301 Moved Permanently
   */
  String MOVED_PERMANENTLY = "301";

  /**
   * 302 Moved Temporarily
   */
  String MOVED_TEMPORARILY = "302";

  /**
   * 303 See Other
   */
  String SEE_OTHER = "303";

  /**
   * 304 Not Modified
   */
  String NOT_MODIFIED = "304";

  /**
   * 305 Use Proxy
   */
  String USE_PROXY = "305";

  /**
   * 307 Temporary Redirect
   */
  String TEMPORARY_REDIRECT = "307";

  //  --- 4xx Client Error ---

  /**
   * 400 Bad Request
   */
  String BAD_REQUEST = "400";

  /**
   * 401 Unauthorized
   */
  String UNAUTHORIZED = "401";

  /**
   * 402 Payment Required
   */
  String PAYMENT_REQUIRED = "402";

  /**
   * 403 Forbidden
   */
  String FORBIDDEN = "403";

  /**
   * 404 Not Found
   */
  String NOT_FOUND = "404";

  /**
   * 405 Method Not Allowed
   */
  String METHOD_NOT_ALLOWED = "405";

  /**
   * 406 Not Acceptable
   */
  String NOT_ACCEPTABLE = "406";

  /**
   * 407 Proxy Authentication Required
   */
  String PROXY_AUTHENTICATION_REQUIRED = "407";

  /**
   * 408 Request Timeout
   */
  String REQUEST_TIMEOUT = "408";

  /**
   * 409 Conflict
   */
  String CONFLICT = "409";

  /**
   * 410 Gone
   */
  String GONE = "410";

  /**
   * 411 Length Required
   */
  String LENGTH_REQUIRED = "411";

  /**
   * 412 Precondition Failed
   */
  String PRECONDITION_FAILED = "412";

  /**
   * 413 Request Entity Too Large
   */
  String REQUEST_TOO_LONG = "413";

  /**
   * 414 Request-URI Too Long
   */
  String REQUEST_URI_TOO_LONG = "414";

  /**
   * 415 Unsupported Media Type
   */
  String UNSUPPORTED_MEDIA_TYPE = "415";

  /**
   * 416 Requested Range Not Satisfiable
   */
  String REQUESTED_RANGE_NOT_SATISFIABLE = "416";

  /**
   * 417 Expectation Failed
   */
  String EXPECTATION_FAILED = "417";

  /**
   * 419 Insufficient Space on Resource or 419 Proxy Reauthentication Required
   */
  String INSUFFICIENT_SPACE_ON_RESOURCE = "419";

  /**
   * 420 Method Failure
   */
  String METHOD_FAILURE = "420";

  /**
   * 422 Unprocessable Entity
   */
  String UNPROCESSABLE_ENTITY = "422";

  /**
   * 423 Locked
   */
  String LOCKED = "423";

  /**
   * 424 Failed Dependency
   */
  String FAILED_DEPENDENCY = "424";

  //  --- 5xx Server Error ---

  /**
   * 500 Server Error
   */
  String INTERNAL_SERVER_ERROR = "500";

  /**
   * 501 Not Implemented
   */
  String NOT_IMPLEMENTED = "501";

  /**
   * 502 Bad Gateway
   */
  String BAD_GATEWAY = "502";

  /**
   * 503 Service Unavailable
   */
  String SERVICE_UNAVAILABLE = "503";

  /**
   * 504 Gateway Timeout
   */
  String GATEWAY_TIMEOUT = "504";

  /**
   * 505 HTTP Version Not Supported
   */
  String HTTP_VERSION_NOT_SUPPORTED = "505";

  /**
   * 507 Insufficient Storage
   */
  String INSUFFICIENT_STORAGE = "507";
}

package com.itv

package aws {

  trait AWSService[Req, Res] extends (Req => Res)

}

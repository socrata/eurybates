package com.socrata
package eurybates

import com.rojoma.json.v3.ast.JValue

case class Message(tag: Tag, details: JValue, activities: JValue)

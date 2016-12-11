package mixit.support

import com.github.jknack.handlebars.springreactive.HelperSource

class IfEqHelperSource : HelperSource<String>("ifEq", { context, options -> if (context == options.params[0]) options.fn.text() else null })
import org.springframework.web.servlet.view.script.RenderingContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

fun render(template: String, model: Map<String, Any>, rc: RenderingContext): String {
    val se = ScriptEngineManager().getEngineByName("kotlin")
    val bindings = SimpleBindings(model)
    return se.eval(template, bindings) as String
}
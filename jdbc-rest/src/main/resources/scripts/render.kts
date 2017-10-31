import org.springframework.web.servlet.view.script.RenderingContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

fun render(template: String, model: Map<String, Any>, renderingContext: RenderingContext)  : String {
    val engine = ScriptEngineManager().getEngineByName("kotlin")
    val bindings = SimpleBindings(model)
    return engine.eval(template, bindings) as String
}
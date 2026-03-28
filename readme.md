# Piper

> A modern, minimal rewrite of FreeMarker with explicit extensibility and zero magic.

Piper keeps the template syntax you already know — but replaces FreeMarker's bean wrappers and built-ins with a clean, type-driven model.

---

## ✨ Why Piper?

FreeMarker is powerful, but:

- Bean wrappers are opaque
- Built-ins are hard coded and not pluggable
- Extending behavior is awkward

**Piper fixes that:**

- **Glue instead of bean wrappers** (explicit, per-type access)
- **Mappers instead of built-ins** (pipe syntax)
- **Pluggable directives** (no hardcoded syntax)
- **Predictable evaluation model**
- **Lightweight and embeddable**

---

## ⚡ Example

```ftl
<#list bean.list as entry>
  ${entry | uppercase}
</#list>
```

```java
Piper piper = Piper.builder()
    .withDefaults()
    .build();

Template template = piper.loadTemplate("test.ftl");

String result = template.apply(Map.of("bean", bean));
```

---

## 🧩 Core Concepts

### Glue (replaces freemarker bean wrappers)

You control how types are accessed:

```java
public interface Glue<T> {
    Object get(T target, String name) throws Exception;
    Object get(T target, int index) throws Exception;
    Object invoke(T target, String name, Object[] args) throws Exception;
}
```

---

### Mappers (replaces freemarker built-ins)

```java
public interface Mapper<T> {
  Object apply(T value, Object[] args);
}
```

FreeMarker:

```ftl
${person.name?upper_case} 
${person.birthDate?string('dd.MM.yyyy')}
${trade.price?string('#,###')}
```

Piper:

```ftl
${person.name | uppercase} 
${person.birthDate | format('dd.MM.yyyy')}
${trade.price | format('#,###')}
```

---

### Directives (fully pluggable)

Directives like `<#if>`, `<#list>`, `<#include>` are **not special** — they are registered just like everything else:

```java
Piper piper = Piper.builder()
    .withDirectiveParser(new IfDirectiveParser())
    .withDirectiveParser(new ListDirectiveParser())
    .withDirectiveParser(new IncludeDirectiveParser())
    .withDirectiveParser(new AssignDirectiveParser())
    ...
    .build();
```

**Why this matters:**
- You can add your own directives
- You can override built-in ones
- No hardcoded directive syntax in the engine

---

## 🧠 Expression Language

```ftl
${bean.number * 4 + bean.number2}
${bean.flag ? 'Y' : 'N'}
${bean.list[1]}
${'a' + 'b'}
```

---

## 🔄 FreeMarker Compatibility

✔ `${...}` expressions  
✔ `<#if>`, `<#elseif>`, `<#else>`  
✔ `<#list>`  
✔ `<#include>`  
✔ `<#assign>`

If you know FreeMarker, you already know Piper.

---

## ⚙️ Getting Started

```java
Piper piper = Piper.builder().withDefaults().build();

Template template = piper.loadTemplate("example.ftl");

String output = template.apply(Map.of("bean", bean));
```

---

## 🔌 Defaults Included

`Piper.builder().withDefaults().build()` gives you:

- Glue for beans, lists, maps
- Mappers (`uppercase`, `format`)
- Numeric operations
- Core directives:
    - `if`
    - `list`
    - `include`
    - `assign`

---

## 🎯 Design Goals

- Familiar like FreeMarker
- Explicit instead of magical
- Fully pluggable (Glue, Mappers, Directives, Converters, BinaryOperations)
- Small and fast

---

## 📝 Status

🚧 Early-stage — APIs may evolve

---

## 📄 License

(TODO)
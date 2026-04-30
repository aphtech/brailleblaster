

-- reusible log function for testing.
-- for testing filter use by :
-- pandoc -t bbx.lua file.docx >file.bbx 
-- this log function will make a log.txt in the file you run the above command in
-- delete file each time because this just appends.
-- todo check for file and delete if exists.
local function logToFile(msg)
  local file, err = io.open("log.txt","a")
  if not file then
      print("Failed to open file: " .. err)
      return
  end
  file:write(msg .. "\n")
  file:close()
end

---roman and alpha functions

--[[
-- for setting ordered list values
Roman conversion section 
    Rules:
    I = 1
    V = 5
    X = 10
    L = 50
    C = 100
    D = 500
    M = 1000

    The symbols "I", "X", "C", and "M" can be repeated 3 times in succession, but no more.
    They may appear more than three times if they appear non-sequentially, such as XXXIX.
    "D", "L", and "V" can never be repeated.
]]--


--roman numerals possible based on order (placement in number - units, tens, hundreds, thousands)
local roman_nums_order = { {"I", "V", "X"}, {"X", "L", "C"}, {"C", "D", "M"}, {"M", "M", "M"} }

local function number_roman(str)
    local result = ""
    if str and type(str) == 'string' and (string.len(str) <= 4) then
        local strLen = string.len(str)
        local revStr = string.reverse(str)

        for i=1,strLen do
            local letter = string.sub(revStr, i, i)
            --probably there's a better way to transform a letter to a digit, but i don't know now
            local digit = string.byte(letter,1) - string.byte('0',1)
            local orderRes = ""

            local symbolReps = 0 --num of times symbol has repeated (must not be > 3)
            local substSymbol = 1 --symbol to substitute with (from roman_nums_order table)
            local substSymbolIncrement = 1 --can be 1 or 2, depending if we are before 5 or after 5 (e.g. if we need to use V or X)
            local j=1
            while j <= digit do
            	orderRes = orderRes .. roman_nums_order[i][substSymbol]
            	symbolReps = symbolReps + 1
            	if symbolReps > 3 then
            	    if i >= 4 then --max number of order thousands supported is 3
            	        orderRes = "MMM"
            	        break
            	    end
            	    orderRes = roman_nums_order[i][substSymbol]
            	    substSymbol = substSymbol + substSymbolIncrement
                 	orderRes = orderRes .. roman_nums_order[i][substSymbol]
                 	symbolReps = 0
			--check if next digit exists in advance  -> to remove substraction possibility
			if j+1 <= digit then
			    orderRes = roman_nums_order[i][substSymbol]
                            j = j+1 --go to next numeral ( e.g. IV -> V )
			    substSymbol = substSymbol - substSymbolIncrement --go back to small units
			    substSymbolIncrement = substSymbolIncrement + 1
			end
            	end
            	j=j+1
            end
            substSymbolIncrement = 1 -- reset increment for next order digit
	    result = string.format("%s%s", orderRes, result)
        end
    end
    return result
end


local function  get_pos_nums(num)
    local pos_nums = {}
    while num ~= 0 do
            table.insert(pos_nums,((num % 26)))
        num = num // 26
        end
    if #pos_nums==0 then 
        return {1}
                  end
        return pos_nums
        end

local function number_alpha(num)
local vals={}
            local letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"}
            if num <=26 then
                return  letters[num]
            end
            vals=get_pos_nums(num)
local             rev_vals={}
for k, v in pairs(vals) do
table.insert(rev_vals,1,letters[v])
end
 return table.concat(rev_vals,"")
end

-- pick type of margin label
function list_number_by_style(style,num)
  local val=""
if style == "Decimal" then
val=num
elseif style == "LowerAlpha" then
val=number_alpha(num)
elseif style == "UpperAlpha" then
val=string.upper(number_alpha(num))
elseif style == "LowerRoman" then
val=string.lower(number_roman(tostring(num)))
elseif style == "UpperRoman" then
val=number_roman(tostring(num))
end
return val
end

--end roman and alpha functions

-- Character escaping
local function escape(s, in_attribute)
  return s:gsub("[<>&\"']",
    function(x)
      if x == '<' then
        return '&lt;'
      elseif x == '>' then
        return '&gt;'
      elseif x == '&' then
        return '&amp;'
      elseif x == '"' then
        return '&quot;'
      elseif x == "'" then
        return '&#39;'
      else
        return x
      end
    end)
end

-- Helper function to convert an attributes table into
-- a string that can be put into HTML tags.
local function attributes(attr)
  local attr_table = {}
  for x,y in pairs(attr) do
    if y and y ~= "" then
      table.insert(attr_table, ' ' .. x .. '="' .. escape(y,true) .. '"')
    end
  end
  return table.concat(attr_table)
end

-- Run cmd on a temporary file containing inp and return result.
local function pipe(cmd, inp)
  local tmp = assert(os.tmpname())
  local tmph = assert(io.open(tmp, "w"))
  if tmph then
    tmph:write(inp)
    tmph:close()
  end
  local outh = io.popen(cmd .. " " .. tmp,"r")
  if outh then
    local result = outh:read("*all")
    outh:close()
    os.remove(tmp)
    return result
  end
end
-- Table to store footnotes, so they can be included at the end.
local notes = {}

-- Blocksep is used to separate block elements.
function Blocksep()
  return ''
end

-- count the number of images
local nImages = 0

_PROMPT = '>'
_PROMPT2 = '>>'


-- get the pandoc command for OS platform
local PANDOCCMD = os.getenv('PANDOCCMD')
if nil == PANDOCCMD then PANDOCCMD='pandoc' end


-- This function is called once for the whole document. Parameters:
-- body is a string, metadata is a table, variables is a table.
-- This gives you a fragment.  You could use the metadata table to
-- fill variables in a custom lua template.  Or, pass `--template=...`
-- to pandoc, and pandoc will add do the template processing as
-- usual.

function Doc(body, metadata, variables)
  local buffer = {}
  local function add(s)
    table.insert(buffer, s)
  end

-- add the appropriate header to the document
local hdr = '<?xml version="1.0" encoding="UTF-8"?>'
hdr = hdr .. '<bbdoc xmlns="http://brailleblaster.org/ns/bb" xmlns:bb="http://brailleblaster.org/ns/bb" xmlns:utd="http://brailleblaster.org/ns/utd">'
hdr = hdr .. '<head><bb:version>6</bb:version><utd:isNormalised>false</utd:isNormalised></head>'
hdr = hdr .. '<SECTION bb:type="ROOT"><SECTION bb:type="OTHER">'
add(hdr)
add(body)
if #notes > 0 then
    add(LineBreak(4))
    add(Header(1,'Footnotes',nil))
    for i,item in pairs(notes) do
    add('<BLOCK utd:overrideStyle="Footnote" bb:type="STYLE">' .. item .. '</BLOCK>')
    end
end
local tail = "</SECTION></SECTION></bbdoc>"
add(tail)
return table.concat(buffer,'\n')
end

function sp()
   return ' '
end

-- The functions that follow render corresponding pandoc elements.
-- s is always a string, attr is always a table of attributes, and
-- items is always an array of strings (the items in a list).
-- Comments indicate the types of other variables.

function Str(s)
  local x,_ = escape(s,nil)
  return x
end

function Space()
  return ' '
end

function SoftBreak()
  return ' '
end

function LineBreak(cnt)
  if cnt == 0 then return '' end
  if nil == cnt then
    cnt = 1
  end
  local lb = '<utd:newLine />'
  return  string.rep(lb,cnt)
end

  -- function to allow for the overlapping of emphases
function CheckBoldItalics(s)
  local targBold    =  'bb:emphasis="BOLD"'
  local targItalics =  'bb:emphasis="ITALICS"'
  local replStr     =  'bb:emphasis="BOLD ITALICS"'
  s = string.gsub(s,targBold,replStr)
  s = string.gsub(s,targItalics,replStr)
  return s
end

function Emph(s)
  s = CheckBoldItalics(s)
  return '<INLINE bb:type="EMPHASIS" bb:emphasis="ITALICS">' .. s .. '</INLINE>'
end

function Strong(s)
  s = CheckBoldItalics(s)
  return '<INLINE bb:type="EMPHASIS" bb:emphasis="BOLD">' .. s .. '</INLINE>'
end

function Subscript(s)
  return '_'.. s
end

function Superscript(s)
  return '^'.. s
end

function SmallCaps(s)
  return Strong(s)
end

function Strikeout(s)
  return s
end

function Link(s, src, title, attr)
  local x,_=escape(src,nil)
  local tag = '<SPAN bb:type="OTHER" href="' .. x .. '" '
  tag = tag .. '>' .. s .. '</SPAN>'
  return tag
end

function Image(s, src, title, attr)
  local x,_=escape(src,nil)
  nImages = nImages+1
  local  imgId = 'img-' .. nImages
  local  beginTag = '<IMAGE bb:id="' .. imgId .. '" bb:type="IMAGE"'
  local altxt = ''
  if nil == s then s = '' end
  if nil == title then title = '' end
  if string.len(s) > 0 then altxt = s end
  if string.len(title) > 0 then
     if string.len(altxt) > 0 then
        altxt = altxt..' and '..title
     else
        altxt = title
     end
  end
  altxt = removeGenericTags(altxt)
  return beginTag .. ' bb:source="' .. x .. '" alt="' .. altxt .. '"/>'
end

function CaptionedImage(src, title, caption, attr)
    return Image(title,src,caption,attr)
end

function Code(s, attr)
  return BlockQuote(s)
end

function InlineMath(s)
     local inp = '\\(' .. s .. '\\)'
     local mml = pandoc.pipe(PANDOCCMD, {"--from=latex", "--to=html", "--mathml"}, inp)
     local a,b = string.find(mml,'<math.*</math>')
     if nil ~= a then
        mml = string.sub(mml,a,b)
     end
     return '<INLINE bb:type="MATHML">' .. mml .. '</INLINE>'
end

function DisplayMath(s)
     local inp = '\\(' .. s .. '\\)'
local mml = pandoc.pipe(PANDOCCMD, {"--from=latex", "--to=html", "--mathml"}, inp)
     local a,b = string.find(mml,'<math.*</math>')
     if nil ~= a then mml = string.sub(mml,a,b) end
     return '<INLINE bb:type="MATHML">' .. mml .. '</INLINE>'
end

function Note(s)
  local num = #notes + 1
  table.insert(notes,num .. '. ' .. removeTags(s))
  return Superscript(num)
end

function Span(s, attr)
  if string.len(s) == 0 then return '' end
  return s
end

function RawInline(format, str)
    return RawBlock(format,str)
end

function Cite(s, cs)
  local ids = {}
  for _,cit in ipairs(cs) do
    table.insert(ids, cit.citationId)
  end
  --return "<span class=\"cite\" data-citation-ids=\"" .. table.concat(ids, ",") ..
  --  "\">" .. s .. "</span>"
  return '<BLOCK bb:type="DEFAULT">(' .. table.concat(ids,",") .. ') ' .. s  .. '</BLOCK>'
end

function Plain(s)
  local t = s
  if (nil == t) or
     (string.len(string.gsub(t,'%s+','')) == 0) or
     (startsWith(t,':::')) then
        t = ''
  else
    t = '<BLOCK bb:type="DEFAULT">' .. s .. '</BLOCK>'
  end
  return t
end

function Para(s)
  local t = s
  if nil == t then t = '' end
  if startsWith(t,':::') then
       t = string.gsub(t,':::','',1)
  end
  local n = string.len(string.gsub(t,'%s+',''))
  if n == 0 then
    t = '<utd:newLine /><utd:newLine />'
  elseif not startsWith(t,'<BLOCK')  then
        t = '<BLOCK bb:type="DEFAULT">' .. s .. '</BLOCK>'
  end
  return t
end

function rmUtdNewLine(item)
    local t = item
    if nil == t then t = '' end
    t = string.gsub(t,'<utd:newLine%s*/>', ' ')
    return t
end

-- lev is an integer, the header level.
function Header(lev, s, attr)
  local t = rmUtdNewLine(s)
  N = 1 -- number of line breaks
  if string.len(t) == 0 then return '' end
  local strTag
  local endTag = '</BLOCK>'
  if lev > 2 then
     strTag = '<BLOCK utd:overrideStyle="Cell 7 Heading" bb:type="STYLE">'
  elseif lev > 1 then
       strTag = '<BLOCK utd:overrideStyle="Cell 5 Heading" bb:type="STYLE">'
  else
     strTag = '<BLOCK utd:overrideStyle="Centered Heading" bb:type="STYLE">'
     N = 1
  end
  return strTag .. t .. endTag
end

function BlockQuote(s)
  local t = LineBreak(2)
  t = t .. '<BLOCK utd:overrideStyle="Displayed Blocked Text" '
  t = t .. 'bb:type="STYLE">' .. s .. '</BLOCK>' .. LineBreak(1)
  return t
end

function HorizontalRule()
  return '<utd:span class="sepline">-----------</utd:span>'
end

function LineBlock(ls)
    return  BlockQuote(ls)
end

function CodeBlock(s, attr)
  -- If code block has class 'dot', pipe the contents through dot
  -- and base64, and include the base64-encoded png as a data: URL.
  if attr.class and string.match(' ' .. attr.class .. ' ',' dot ') then
    local png = pipe("base64", pipe("dot -Tpng", s))
    return Image(s, "data:image/png;base64," .. png, '', attr)
  -- otherwise treat as code (one could pipe through a highlighter)
  else
    return  BlockQuote(s)
  end
end

function BulletList(items)
  local listItems = ''
  local beginTag  = '<CONTAINER bb:type="LIST" bb:listType="NORMAL" bb:listLevel="0">'
  local endTag    = '</CONTAINER>'
  local bullet    = items.bullet ~= nil
  for j,item in pairs(items) do
    if j ~= 'bullet' then
        if not bullet then 
            if item ~= nil and string.len(item) > 0 then
                item = removeTags(item)
                listItems = listItems .. '<BLOCK bb:type="LIST_ITEM" bb:itemLevel="0">&#8226;&#32;' .. item .. '</BLOCK>'
            end
        else
            listItems = listItems .. item
        end 
    end
  end
  local val = ''
  if string.len(listItems) > 0 then
    val = beginTag .. listItems .. endTag
  end
  return val
end

function OrderedList(items,start,style)
  local newItems = {}
  local listItems = ''
  local itemCtr = start
  local beginTag  = '<CONTAINER bb:type="LIST" bb:listType="NORMAL" bb:listLevel="0">'
   local endTag    = '</CONTAINER>'
  for _, item in pairs(items) do
    if nil ~= item and string.len(item) > 0 then
      item = removeTags(item)
      local lev = list_number_by_style(style,itemCtr) 
      table.insert(newItems,'<BLOCK bb:type="LIST_ITEM" bb:itemLevel="0">'.. lev .. '. ' .. item .. '</BLOCK>')
      itemCtr = itemCtr + 1
    end
  end
  local val = ''
 -- if string.len(listItems) > 0 then
 --   val = beginTag .. listItems .. endTag
 -- end
  newItems.bullet = true

  return BulletList(newItems)
end

-- Render a definition list. Pandoc passes items as an array of {term, defs}
-- pairs where term is a string and defs is an array of strings.
-- Delegating directly to BulletList crashed because BulletList expects
-- items to be an array of strings, not {term, defs} tables.
-- Uses the proper BBX DEFINITION list type with DEFINITION_TERM spans,
-- matching how the NIMAS importer handles <dl>/<dt>/<dd> elements.
-- In Pandoc 3.x, DefinitionList items are structured as an array of single-key tables:
-- each element is { ["term string"] = { def1_string, def2_string, ... } }
-- (Pandoc 2.x used { term_string, { def1_string, ... } } with numeric indices)
function DefinitionList(items)
    local buffer = {}
    -- bb:listLevel="0" is required by FixNestedList (getAttribute("listLevel") crashes without it)
    local beginTag = '<CONTAINER bb:type="LIST" bb:listType="DEFINITION" bb:listLevel="0">'
    local endTag   = '</CONTAINER>'
    for _, item in ipairs(items) do
        -- In Pandoc 3.x each item is {[term_string] = defs_table}
        for term, defs in pairs(item) do
            local termText = ''
            if term ~= nil then
                termText = removeTags(term)
            end
            if defs ~= nil then
                local firstDef = true
                for _, def in ipairs(defs) do
                    if def ~= nil then
                        local d = removeTags(def)
                        if d ~= nil and string.len(string.gsub(d, '%s+', '')) > 0 then
                            if firstDef and string.len(termText) > 0 then
                                -- First definition: include the term as a DEFINITION_TERM span
                                -- bb:itemLevel="0" is required by FixNestedList
                                buffer[#buffer+1] = '<BLOCK bb:type="LIST_ITEM" bb:itemLevel="0">'
                                    .. '<SPAN bb:type="DEFINITION_TERM">' .. termText .. '</SPAN>'
                                    .. ' ' .. d
                                    .. '</BLOCK>'
                            else
                                -- Continuation definitions (no term)
                                buffer[#buffer+1] = '<BLOCK bb:type="LIST_ITEM" bb:itemLevel="0">' .. d .. '</BLOCK>'
                            end
                            firstDef = false
                        end
                    end
                end
                -- Term with no definitions still needs to be emitted
                if firstDef and string.len(termText) > 0 then
                    buffer[#buffer+1] = '<BLOCK bb:type="LIST_ITEM" bb:itemLevel="0">'
                        .. '<SPAN bb:type="DEFINITION_TERM">' .. termText .. '</SPAN>'
                        .. '</BLOCK>'
                end
            elseif string.len(termText) > 0 then
                buffer[#buffer+1] = '<BLOCK bb:type="LIST_ITEM" bb:itemLevel="0">'
                    .. '<SPAN bb:type="DEFINITION_TERM">' .. termText .. '</SPAN>'
                    .. '</BLOCK>'
            end
        end
    end
    local val = ''
    if #buffer > 0 then
        val = beginTag .. table.concat(buffer) .. endTag
    end
    return val
end

-- Convert pandoc alignment to something HTML can use.
-- align is AlignLeft, AlignRight, AlignCenter, or AlignDefault.
function html_align(align)
       return ''
end

-- Caption is a string, aligns is an array of strings,
-- widths is an array of floats, headers is an array of
-- strings, rows is an array of arrays of strings.
function head_row(str)
    return str
end

function Table(caption, aligns, widths, headers, rows)
  local buffer = {}
  local utdBuffer = {}
  local function add(s)
    table.insert(buffer, s)
  end
  local function utdAdd(s)
    table.insert(utdBuffer, s)
  end
  -- put the beginning of the table in place
  add('<CONTAINER bb:type="TABLE" format="simple">')
  local utd = '<CONTAINER bb:type="TABLE" format="simple" '
  utd = utd .. 'utd:tableCopy="true" class="utd:tableSimple">'
  utdAdd(utd)
  -- now deal with headers
  local header_row = {}
  if headers ~= nil or #headers > 0 then
     for _,h in pairs(headers) do
        h = removeTags(h)
        table.insert(header_row,h)
     end
     table.insert(rows,1,header_row)
  end
  local hdrline = '<utd:span class="sepline">------------</utd:span>'
  local tr      = '<CONTAINER bb:type="TABLE_ROW" bb:rowType="NORMAL">'
  local tr_end  = '</CONTAINER>'
  local rcell   = '<BLOCK bb:type="TABLE_CELL">'
  local rcell_end = '</BLOCK>'
  local empty_cell = '<BLOCK bb:type="TABLE_CELL"/>'
  local rcell_utd  = '<BLOCK bb:type="TABLE_CELL" row-col="'
  local rcell_utd_empty = '/>'

  local j,k,rowcol,rcend

  j = 0 -- row counter
  k = 0 -- col counter
  for _, row in pairs(rows) do
    add(tr)
    utdAdd(tr)
    for i,c in pairs(row) do
        c = removeTags(c)
        rowcol = j .. '-' .. k .. '"'
        if nil == c or c == "" then -- an empty cell
           add(empty_cell)
           utdAdd(rcell_utd .. rowcol .. rcell_utd_empty)
        else
           rowcol = rowcol .. '>'
           add(rcell .. c .. rcell_end)
           if j == 0 then
              utdAdd(rcell_utd .. rowcol .. c .. hdrline .. rcell_end)
           else
              utdAdd(rcell_utd .. rowcol .. c .. rcell_end)
           end
        end
        k = k + 1
    end
    add(tr_end)
    utdAdd(tr_end)
    j = j + 1
    k = 0
  end
  add('</CONTAINER>')
  utdAdd('</CONTAINER>')
  for _,x in pairs(utdBuffer) do
    table.insert(buffer,x)
  end
  return table.concat(buffer)
end

function RawBlock(format, str)
    local t = '<BLOCK bb:type="DEFAULT">'
    local x = removeGenericTags(str)
    local y = string.gsub(x,'%s+','')
    local w = string.len(y)
    if w > 0 then
        t = t..x..'</BLOCK>'
    else
        t = ''
    end
    return t
end

function Div(s, attr)
  return s
end

function startsWith(s,p)
    local itDoes = false
    if nil ~= s and nil ~= p and string.len(s) > 0 then
        local itDoes = string.sub(s,1,string.len(p)) == p
    end
    return itDoes
end

function removeGenericTags(item)
    local t = item 
    if nil == t then t = '' end
    t = string.gsub(t,'<.->','')
    if nil == t then t = '' end
    return t
end

function removeTags(item)
  local lists = {}
  local pContainer = '<%s*CONTAINER%s+.->.+</CONTAINER>'
  local pList      = '<LIST>'
  for x in string.gmatch(item,pContainer) do
       table.insert(lists,x)
  end
  local t = string.gsub(item,pContainer,pList)
  t = string.gsub(t,'<BLOCK.->','')
  t = string.gsub(t,'</BLOCK>','')
  t = string.gsub(t,'<utd.->','')
  for i,j in pairs(lists) do
    local escapedJ = j:gsub('%%', '%%%%')  -- Escape any % characters in j
    t = string.gsub(t, pList, escapedJ, 1)
end
return t
end

-- Double Quoted
function DoubleQuoted(content)
   return Str('"' .. content .. '"')
end

-- Single Quoted
function SingleQuoted(content)
   return Str("'" .. content .. "'")
end

-- The following code will produce runtime warnings when you haven't defined
-- all of the functions you need for the custom writer, so it's useful
-- to include when you're working on a writer.
local meta = {}
meta.__index =
  function(_, key)
    io.stderr:write(string.format("WARNING: Undefined function '%s'\n",key))
    return function() return "" end
  end
setmetatable(_G, meta)

'use strict';
(() => {
  const $ = id => document.getElementById(id);
  const state = {tab:'clientes', clientes:[], contas:[], page:0, size:8, search:'', loaded:false, loading:false, editing:null, deleting:null, busy:false};
  const money = value => new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(value);
  const singular = tab => tab === 'clientes' ? 'cliente' : 'conta';
  let toastTimer;
  function toast(message,error=false) { clearTimeout(toastTimer); $('toast').textContent=message; $('toast').classList.toggle('error',error); $('toast').hidden=false; toastTimer=setTimeout(()=>$('toast').hidden=true,7000); }
  function errorBox(id,message='') { $(id).textContent=message; $(id).hidden=!message; }
  async function api(path,options={}) {
    const controller=new AbortController(); const timer=setTimeout(()=>controller.abort(),60000);
    try {
      const response=await fetch(path,{...options,signal:controller.signal,headers:{...(options.body?{'Content-Type':'application/json'}:{}),...options.headers}});
      const body=response.status===204?null:await response.json().catch(()=>null);
      if(!response.ok) {
        const fields=body?.campos ? Object.entries(body.campos).map(([name,value])=>`${name}: ${value}`).join('; ') : '';
        throw new Error([body?.detail || `Não foi possível concluir a operação (HTTP ${response.status}).`,fields].filter(Boolean).join(' '));
      }
      return body;
    } catch(error) {
      if(error.name==='AbortError') throw new Error('A aplicação demorou para responder. Aguarde a retomada do banco e tente novamente.');
      if(error instanceof TypeError) throw new Error('Não foi possível conectar à aplicação. Verifique sua conexão e tente novamente.');
      throw error;
    } finally {clearTimeout(timer);}
  }
  async function allRecords(entity) {
    let page=0,records=[];
    while(true) {
      const data=await api(`/api/${entity}?page=${page}&size=100&sort=id,asc`);
      if(!data || !Array.isArray(data.content)) throw new Error('A aplicação retornou uma lista inválida.');
      records.push(...data.content);
      if(data.last===true || data.content.length===0 || (typeof data.totalPages==='number' && page+1>=data.totalPages)) break;
      page++;
    }
    return records;
  }
  function connection(online) { $('connection-dot').className=`connection-dot ${online?'online':'offline'}`; $('connection-label').textContent=online?'Aplicação conectada':'Conexão indisponível'; }
  async function load() {
    if(state.loading) return;
    state.loading=true; $('refresh').disabled=true; $('refresh').textContent='Atualizando…'; document.body.classList.add('busy');
    errorBox('page-error');render();
    try {
      const [clientes,contas]=await Promise.all([allRecords('clientes'),allRecords('contas')]);
      state.clientes=clientes; state.contas=contas; state.loaded=true; connection(true);
      $('stat-clientes').textContent=clientes.length; $('stat-contas').textContent=contas.length;
      // Soma em centavos para evitar erro binário na apresentação dos saldos.
      $('stat-saldo').textContent=money(contas.reduce((sum,c)=>sum+Math.round(Number(c.saldo)*100),0)/100);
      $('nav-clientes').textContent=clientes.length; $('nav-contas').textContent=contas.length;
      return true;
    } catch(error) {connection(false);errorBox('page-error',`${error.message}${state.loaded?' Os dados exibidos são da última atualização bem-sucedida.':''}`);return false;}
    finally {state.loading=false;$('refresh').disabled=false;$('refresh').textContent='↻ Atualizar dados';document.body.classList.remove('busy');render();}
  }
  function make(tag,cls,text) {const element=document.createElement(tag);if(cls)element.className=cls;if(text!==undefined)element.textContent=text;return element;}
  function clienteNome(id) {return state.clientes.find(c=>c.id===id)?.nome || `Cliente #${id}`;}
  function filtered() {
    const q=state.search.toLocaleLowerCase('pt-BR');
    return state[state.tab].filter(r=> (state.tab==='clientes'?[r.id,r.nome,r.email]:[r.id,r.numero,r.tipo,r.clienteId,clienteNome(r.clienteId)]).join(' ').toLocaleLowerCase('pt-BR').includes(q));
  }
  function render() {
    const clients=state.tab==='clientes';
    $('records-title').textContent=clients?'Clientes':'Contas';$('records-subtitle').textContent=clients?'Pessoas por trás de cada conta.':'Cada conta conectada ao cliente certo.';
    $('new-record').textContent=clients?'＋ Novo cliente':'＋ Nova conta';$('new-record').disabled=state.loading||(!clients && (!state.loaded||state.clientes.length===0));
    $('new-record').title=!clients&&state.clientes.length===0?'Cadastre um cliente antes de criar uma conta.':'';
    $('search').placeholder=clients?'Buscar por nome, email ou ID':'Buscar por número, cliente, tipo ou ID';$('search').setAttribute('aria-label',clients?'Buscar clientes':'Buscar contas');
    document.querySelectorAll('[data-tab]').forEach(button=>{const active=button.dataset.tab===state.tab;button.classList.toggle('active',active);if(active)button.setAttribute('aria-current','page');else button.removeAttribute('aria-current');});
    const records=filtered(), pages=Math.max(1,Math.ceil(records.length/state.size));state.page=Math.min(state.page,pages-1);
    const start=state.page*state.size;
    $('table-head').replaceChildren();const header=make('tr');
    (clients?['ID','CLIENTE','EMAIL','AÇÕES']:['ID','CONTA','CLIENTE','TIPO','SALDO','AÇÕES']).forEach(label=>{const th=make('th','',label);th.scope='col';header.append(th);});$('table-head').append(header);
    const tbody=$('table-body');tbody.replaceChildren();$('empty-state').hidden=true;
    if(!state.loaded) {
      const row=make('tr'),cell=make('td','loading-row',state.loading?'Carregando seus cadastros…':'Não foi possível carregar os registros. Clique em Atualizar dados.');cell.colSpan=clients?4:6;row.append(cell);tbody.append(row);
    } else if(records.length===0) {
      $('empty-state').hidden=false;
      $('empty-title').textContent=state.search?'Nenhum resultado encontrado':clients?'Tudo pronto para começar':'Suas contas começam aqui';
      $('empty-description').textContent=state.search?'Tente buscar por outro termo.':clients?'Adicione seu primeiro cliente para depois vincular uma conta.':state.clientes.length?'Crie uma conta e vincule-a a um cliente cadastrado.':'Cadastre um cliente antes de criar sua primeira conta.';
    } else {
      for(const record of records.slice(start,start+state.size)) {
        const row=make('tr');row.append(make('td','',`#${record.id}`));
        if(clients) {
          const cell=make('td'),person=make('div','person-cell');person.append(make('span','avatar',record.nome.split(/\s+/).filter(Boolean).slice(0,2).map(word=>word[0]).join('').toLocaleUpperCase('pt-BR')),make('span','person-name',record.nome));cell.append(person);row.append(cell,make('td','',record.email));
        } else {
          row.append(make('td','person-name',record.numero),make('td','',clienteNome(record.clienteId)));const type=make('td');type.append(make('span',`type-badge ${record.tipo==='POUPANCA'?'savings':''}`,record.tipo==='POUPANCA'?'Poupança':'Corrente'));row.append(type,make('td','',money(record.saldo)));
        }
        const actions=make('td'),container=make('div','row-actions');
        for(const [action,glyph,label] of [['view','↗','Ver'],['edit','✎','Editar'],['delete','×','Excluir']]) {
          const b=make('button',`row-button ${action}`,glyph);b.type='button';b.title=`${label} ${singular(state.tab)} #${record.id}`;b.setAttribute('aria-label',b.title);b.addEventListener('click',()=>operate(action,record,b));container.append(b);
        }
        actions.append(container);row.append(actions);tbody.append(row);
      }
    }
    $('record-total').textContent=state.loaded?`${records.length} ${records.length===1?'registro':'registros'}${state.search?(records.length===1?' encontrado':' encontrados'):''}`:'Carregando registros…';
    $('table-range').textContent=state.loaded?(records.length?`Mostrando ${start+1}–${Math.min(start+state.size,records.length)} de ${records.length}`:'Nenhum registro'):'Aguardando conexão';
    $('page-number').textContent=`${state.page+1} / ${pages}`;$('prev').disabled=state.page===0||!state.loaded;$('next').disabled=state.page>=pages-1||!state.loaded;
  }
  function addField(label,name,{value='',type='text',maxLength,pattern,min,step,options,help}={}) {
    const wrapper=make('label','field',label),input=document.createElement(options?'select':'input');input.name=name;input.id=`field-${name}`;input.required=true;
    if(options){for(const o of options){const option=make('option','',o.label);option.value=o.value;input.append(option);}}
    else {input.type=type;if(maxLength)input.maxLength=maxLength;if(pattern)input.pattern=pattern;if(min!==undefined)input.min=min;if(step)input.step=step;}
    input.value=String(value);wrapper.append(input);if(help)wrapper.append(make('span','field-hint',help));$('form-fields').append(wrapper);return input;
  }
  function editor(record=null) {
    state.editing={tab:state.tab,id:record?.id??null};$('record-form').reset();$('form-fields').replaceChildren();errorBox('form-error');
    const entity=singular(state.tab);$('editor-title').textContent=`${record?'Editar':'Novo'} ${entity}`;$('save').textContent=`Salvar ${entity}`;
    if(state.tab==='clientes'){addField('Nome','nome',{value:record?.nome,maxLength:120});addField('Email','email',{value:record?.email,type:'email',maxLength:160});}
    else {
      addField('Número da conta','numero',{value:record?.numero,maxLength:20,pattern:'[0-9]{4,20}',help:'De 4 a 20 dígitos. O número deve ser único.'});
      addField('Cliente','clienteId',{value:record?.clienteId??'',options:[{value:'',label:'Selecione um cliente'},...state.clientes.map(c=>({value:c.id,label:`${c.nome} · #${c.id}`}))]});
      addField('Tipo de conta','tipo',{value:record?.tipo??'CORRENTE',options:[{value:'CORRENTE',label:'Corrente'},{value:'POUPANCA',label:'Poupança'}]});
      addField('Saldo cadastrado (R$)','saldo',{value:record?.saldo??'0.00',type:'number',min:'0',step:'0.01',help:'Valor não negativo, com até duas casas decimais.'});
    }
    $('editor').showModal();
  }
  async function operate(action,record,button) {
    if(state.busy)return;
    if(action==='delete'){state.deleting={tab:state.tab,id:record.id};errorBox('delete-error');$('delete-description').textContent=`Você vai excluir ${singular(state.tab)} #${record.id}${state.tab==='clientes'?` (${record.nome})`:` (${record.numero})`}.`;$('confirmation').showModal();return;}
    const tab=state.tab;button.disabled=true;
    try {const current=await api(`/api/${tab}/${record.id}`);if(state.tab!==tab)return;if(action==='edit')editor(current);else showDetails(current);}
    catch(error){toast(error.message,true);}finally{button.disabled=false;}
  }
  function showDetails(record) {
    $('details-title').textContent=`${state.tab==='clientes'?'Cliente':'Conta'} #${record.id}`;const dl=$('details-fields');dl.replaceChildren();
    const entries=state.tab==='clientes'?[['ID',record.id],['Nome',record.nome],['Email',record.email]]:[['ID',record.id],['Número',record.numero],['Cliente',`${clienteNome(record.clienteId)} (#${record.clienteId})`],['Tipo',record.tipo==='POUPANCA'?'Poupança':'Corrente'],['Saldo',money(record.saldo)]];
    entries.forEach(([name,value])=>dl.append(make('dt','',name),make('dd','',String(value))));$('details').showModal();
  }
  function lockDialog(id,locked) {$(id).querySelectorAll('button,input,select').forEach(control=>control.disabled=locked);state.busy=locked;}
  for(const id of ['editor','confirmation']) $(id).addEventListener('cancel',event=>{if(state.busy)event.preventDefault();});
  $('record-form').addEventListener('submit',async event=>{
    event.preventDefault();if(state.busy)return;const form=new FormData(event.currentTarget),editing={...state.editing};
    const payload=editing.tab==='clientes'?{nome:String(form.get('nome')).trim(),email:String(form.get('email')).trim()}:{numero:String(form.get('numero')).trim(),clienteId:Number(form.get('clienteId')),tipo:form.get('tipo'),saldo:Number(form.get('saldo'))};
    errorBox('form-error');lockDialog('editor',true);$('save').textContent='Salvando…';
    let success=false;
    try {await api(`/api/${editing.tab}${editing.id?`/${editing.id}`:''}`,{method:editing.id?'PUT':'POST',body:JSON.stringify(payload)});success=true;$('editor').close();state.page=0;toast(`${editing.tab==='clientes'?'Cliente':'Conta'} ${editing.id?'atualizado':'criado'} com sucesso.`);}
    catch(error){errorBox('form-error',error.message);}finally{lockDialog('editor',false);$('save').textContent=`Salvar ${singular(editing.tab)}`;}
    if(success && !await load())toast('Registro salvo. Não foi possível atualizar a lista; clique em Atualizar dados.',true);
  });
  $('delete-form').addEventListener('submit',async event=>{
    event.preventDefault();if(state.busy)return;const record={...state.deleting};errorBox('delete-error');lockDialog('confirmation',true);$('confirm-delete').textContent='Excluindo…';let success=false;
    try{await api(`/api/${record.tab}/${record.id}`,{method:'DELETE'});success=true;$('confirmation').close();toast('Registro excluído com sucesso.');}
    catch(error){errorBox('delete-error',error.message);}finally{lockDialog('confirmation',false);$('confirm-delete').textContent='Excluir registro';}
    if(success && !await load())toast('Registro excluído. Não foi possível atualizar a lista; clique em Atualizar dados.',true);
  });
  document.querySelectorAll('[data-close]').forEach(button=>button.addEventListener('click',()=>{if(!state.busy)$(button.dataset.close).close();}));
  document.querySelectorAll('[data-tab]').forEach(button=>button.addEventListener('click',()=>{state.tab=button.dataset.tab;state.search='';state.page=0;$('search').value='';render();}));
  $('search').addEventListener('input',event=>{state.search=event.target.value;state.page=0;render();});
  $('prev').addEventListener('click',()=>{state.page--;render();});$('next').addEventListener('click',()=>{state.page++;render();});
  $('new-record').addEventListener('click',()=>editor());$('refresh').addEventListener('click',()=>load());
  load();
})();

package com.soleador.Administracion.Soleador.controladores;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.soleador.Administracion.Soleador.Error.WebException;
import com.soleador.Administracion.Soleador.Util.Textos;
import com.soleador.Administracion.Soleador.entidades.ItemPedido;
import com.soleador.Administracion.Soleador.entidades.Pedido;
import com.soleador.Administracion.Soleador.enumeraciones.EstadoPedido;
import com.soleador.Administracion.Soleador.models.PedidoModel;
import com.soleador.Administracion.Soleador.servicios.ClienteService;
import com.soleador.Administracion.Soleador.servicios.PedidoService;
import com.soleador.Administracion.Soleador.servicios.ProductoVentaService;
import com.soleador.Administracion.Soleador.servicios.TransporteService;

@Controller
//@PreAuthorize("hasRole('ROLE_ADMINISTRADOR')")
@RequestMapping("/pedido")
public class PedidoController extends Controlador {

	@Autowired
	private PedidoService pedidoService;

	@Autowired
	private ClienteService clienteService;

	@Autowired
	private TransporteService transporteService;

	@Autowired
	private ProductoVentaService productoVentaService;

	public PedidoController() {
		super("pedido-list", "pedido-form");
	}

	@PostMapping("/guardar")
	public String guardar(HttpSession session, @Valid @ModelAttribute(Textos.PEDIDO_LABEL) Pedido pedido,
			BindingResult resultado, ModelMap modelo,
			@RequestParam(required = false, name = "itemPedido") List<ItemPedido> itemPedido) {
		log.info("METODO: pedido.guardar -- PARAMETROS: " + pedido);
		try {
			if (resultado.hasErrors()) {
				error(modelo, resultado);
			} else {
				pedidoService.guardar(pedido);
				return "redirect:/pedido/listado";
			}
		} catch (WebException e) {
			modelo.addAttribute(Textos.ERROR, "Ocurrió un error al intentar modificar el pedido. " + e.getMessage());
		} catch (Exception e) {
			modelo.addAttribute(Textos.ERROR, "Ocurrió un error inesperado al intentar modificar el pedido.");
			log.error(Textos.ERROR_INESPERADO, e);
		}
		return vistaFormulario;
	}

	@PostMapping("/eliminar")
	public String eliminar(@ModelAttribute(Textos.PEDIDO_LABEL) Pedido pedido, ModelMap model) {
		log.info("METODO: pedido.eliminar() -- PARAMETROS: " + pedido);
		model.addAttribute(Textos.ACCION_LABEL, "eliminar");
		try {
			pedidoService.eliminar(pedido.getId());
			return "redirect:/pedido/listado";
		} catch (Exception e) {
			model.addAttribute(Textos.ERROR, "Ocurrió un error inesperado al intentar eliminar el pedido.");
			return vistaFormulario;
		}
	}

	@GetMapping("/formulario")
	public ModelAndView formulario(@RequestParam(required = false) String id,
			@RequestParam(required = false) String accion) {

		ModelAndView modelo = new ModelAndView(vistaFormulario);
		PedidoModel pedido = new PedidoModel();

		if (accion == null || accion.isEmpty()) {
			accion = Textos.GUARDAR_LABEL;
		}

		if (id != null) {
			pedido = pedidoService.buscar(id);
		} else {
			pedido.setFechaPedido(new Date());
			pedido.setNumeroPedido(pedidoService.obtenerNumeroPedido());
		}
		modelo.addObject(Textos.CLIENTES_LABEL, clienteService.listarActivos());
		modelo.addObject(Textos.TRANSPORTES_LABEL, transporteService.listarActivos());
		modelo.addObject(Textos.PEDIDO_LABEL, pedido);
		modelo.addObject(Textos.PRODUCTO_VENTA_LABEL, productoVentaService.listarActivos());
		modelo.addObject(Textos.ACCION_LABEL, accion);
		return modelo;
	}

	@GetMapping("/listado")
	public ModelAndView listarPedidosPendientes(HttpSession session, Pageable paginable,
			@RequestParam(required = false) String q) {

		log.info("METODO: pedido.listarPedidosPendientes() -- PARAMETROS: " + paginable);

		ModelAndView modelo = new ModelAndView(vistaListado);
		ordenar(paginable, modelo);

		Page<Pedido> page = pedidoService.listarActivos(paginable, q);
		if (q != null && !q.isEmpty()) {
			modelo.addObject(Textos.QUERY_LABEL, q);
		}
		modelo.addObject(Textos.PAGE_LABEL, page);
		modelo.addObject(Textos.URL_LABEL, "/pedido/listado");
		modelo.addObject(Textos.PEDIDO_LABEL, new Pedido());

		return modelo;
	}

	@GetMapping("/listadoPorEstado")
	public ModelAndView listarPedidosPorEstado(HttpSession session, Pageable paginable,
			@RequestParam(required = false) String estado) {

		log.info("METODO: pedido.listarPedidosPorEstado() -- PARAMETROS: " + paginable);

		ModelAndView modelo = new ModelAndView(vistaListado);
		ordenar(paginable, modelo);
		Page<Pedido> page = null;
		if (estado == null || estado.isEmpty()) {
			page = pedidoService.listarActivos(paginable, null);
		} else {
			page = pedidoService.listarPedidosActivosPorEstadoPedido(paginable, null, EstadoPedido.valueOf(estado));
		}

		modelo.addObject("estado", estado);
		modelo.addObject(Textos.PAGE_LABEL, page);
		modelo.addObject(Textos.URL_LABEL, "/pedido/listado");
		modelo.addObject(Textos.PEDIDO_LABEL, new Pedido());

		return modelo;
	}

}
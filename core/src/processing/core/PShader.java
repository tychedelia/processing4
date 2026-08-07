/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-21 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;

/**
 * This class encapsulates a shader program, including a vertex and a fragment
 * shader. It is the renderer-agnostic type that sketches declare and pass to
 * <b>shader()</b> and <b>filter()</b>; each renderer supplies its own
 * implementation (for example a GLSL-backed shader under P2D/P3D, or a WGSL
 * one under the WebGPU renderer).
 *
 * Use the <b>loadShader()</b> function to load your shader code, rather than
 * constructing a renderer-specific implementation directly.
 *
 * @webref rendering:shaders
 * @webBrief This class encapsulates a shader program, including a vertex and a
 *           fragment shader
 */
public interface PShader extends PUniforms {
}
